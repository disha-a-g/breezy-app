package com.whirlyfan.breezy.viewmodels

import android.util.Log
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.PostRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PostInteractionManager private constructor(
    private val api: BreezyAPI
) {
    private val postRequests = PostRequests(api)

    // Map of postId to isLiked status
    private val _likedPosts = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedPosts: StateFlow<Map<String, Boolean>> = _likedPosts.asStateFlow()

    // Map of postId to like count
    private val _likeCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val likeCounts: StateFlow<Map<String, Long>> = _likeCounts.asStateFlow()

    // Map of postId to comment count
    private val _commentCounts = MutableStateFlow<Map<String, Long>>(emptyMap())
    val commentCounts: StateFlow<Map<String, Long>> = _commentCounts.asStateFlow()

    private val _loadingLikePosts = MutableStateFlow<Set<String>>(emptySet())
    val loadingLikePosts: StateFlow<Set<String>> = _loadingLikePosts.asStateFlow()

    private fun setPostLikeLoading(postId: String, isLoading: Boolean) {
        _loadingLikePosts.value = if (isLoading) {
            _loadingLikePosts.value + postId
        } else {
            _loadingLikePosts.value - postId
        }
    }

    fun updateCommentCount(postId: String, count: Long) {
        _commentCounts.value = _commentCounts.value.toMutableMap().apply {
            put(postId, count)
        }
    }

    fun updateLikeStatus(postId: String, isLiked: Boolean) {
        _likedPosts.value = _likedPosts.value.toMutableMap().apply {
            put(postId, isLiked)
        }
    }

    fun updateLikeCount(postId: String, count: Long) {
        _likeCounts.value = _likeCounts.value.toMutableMap().apply {
            put(postId, count)
        }
    }

    fun incrementCommentCount(postId: String) {
        val currentCount = _commentCounts.value[postId] ?: 0
        updateCommentCount(postId, currentCount + 1)
    }

    fun decrementCommentCount(postId: String) {
        val currentCount = _commentCounts.value[postId] ?: 1
        val newCount = maxOf(0, currentCount - 1) // Ensure count doesn't go below 0
        updateCommentCount(postId, newCount)
    }

    suspend fun likePost(postId: String): Boolean {
        try {
            setPostLikeLoading(postId, true)
            val result = postRequests.likePost(postId)
            if (result) {
                val currentCount = _likeCounts.value[postId] ?: 0
                updateLikeStatus(postId, true)
                updateLikeCount(postId, currentCount + 1)
            }
            return result
        } catch (e: Exception) {
            Log.e("LikeManager", "Error liking post", e)
            return false
        } finally {
            setPostLikeLoading(postId, false)
        }
    }

    suspend fun unlikePost(postId: String): Boolean {
        try {
            setPostLikeLoading(postId, true)
            val result = postRequests.unlikePost(postId)
            if (result) {
                val currentCount = _likeCounts.value[postId] ?: 1
                updateLikeStatus(postId, false)
                updateLikeCount(postId, currentCount - 1)
            }
            return result
        } catch (e: Exception) {
            Log.e("LikeManager", "Error unliking post", e)
            return false
        } finally {
            setPostLikeLoading(postId, false)
        }
    }

    // Register posts with their current like status and counts and comment counts
    fun registerPosts(posts: List<com.whirlyfan.breezy.api.Post>) {
        val likeStatusMap = mutableMapOf<String, Boolean>()
        val likeCountsMap = mutableMapOf<String, Long>()
        val commentCountsMap = mutableMapOf<String, Long>()

        posts.forEach { post ->
            likeStatusMap[post.id] = post.isLiked
            likeCountsMap[post.id] = post.likesCount ?: 0
            commentCountsMap[post.id] = post.commentsCount ?: 0
        }

        _likedPosts.value = _likedPosts.value.toMutableMap().apply { putAll(likeStatusMap) }
        _likeCounts.value = _likeCounts.value.toMutableMap().apply { putAll(likeCountsMap) }
        _commentCounts.value =
            _commentCounts.value.toMutableMap().apply { putAll(commentCountsMap) }
    }

    companion object {
        @Volatile
        private var INSTANCE: PostInteractionManager? = null

        fun getInstance(api: BreezyAPI): PostInteractionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PostInteractionManager(api).also { INSTANCE = it }
            }
        }
    }
}