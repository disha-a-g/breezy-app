package com.whirlyfan.breezy.viewmodels

import android.util.Log
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Comment
import com.whirlyfan.breezy.api.CommentRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommentInteractionManager private constructor(
    private val api: BreezyAPI
) {
    private val commentRequests = CommentRequests(api)

    // Liked status by commentId
    private val _likedComments = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedComments: StateFlow<Map<String, Boolean>> = _likedComments.asStateFlow()

    // Like counts by commentId
    private val _likeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likeCounts: StateFlow<Map<String, Int>> = _likeCounts.asStateFlow()

    // Loading state by commentId
    private val _loadingLikeComments = MutableStateFlow<Set<String>>(emptySet())
    val loadingLikeComments: StateFlow<Set<String>> = _loadingLikeComments.asStateFlow()

    private fun updateLikeStatus(commentId: String, isLiked: Boolean) {
        _likedComments.value = _likedComments.value.toMutableMap().apply {
            put(commentId, isLiked)
        }
    }

    private fun updateLikeCount(commentId: String, count: Int) {
        _likeCounts.value = _likeCounts.value.toMutableMap().apply {
            put(commentId, count)
        }
    }

    private fun setCommentLikeLoading(commentId: String, isLoading: Boolean) {
        _loadingLikeComments.value = if (isLoading) {
            _loadingLikeComments.value + commentId
        } else {
            _loadingLikeComments.value - commentId
        }
    }

    suspend fun likeComment(commentId: String): Boolean {
        try {
            setCommentLikeLoading(commentId, true)
            val result = commentRequests.likeComment(commentId)
            if (result) {
                val currentCount = _likeCounts.value[commentId] ?: 0
                updateLikeStatus(commentId, true)
                updateLikeCount(commentId, currentCount + 1)
            }
            return result
        } catch (e: Exception) {
            Log.e("CommentManager", "Error liking comment", e)
            return false
        } finally {
            setCommentLikeLoading(commentId, false)
        }
    }

    suspend fun unlikeComment(commentId: String): Boolean {
        try {
            setCommentLikeLoading(commentId, true)
            val result = commentRequests.unlikeComment(commentId)
            if (result) {
                val currentCount = _likeCounts.value[commentId] ?: 1
                updateLikeStatus(commentId, false)
                updateLikeCount(commentId, currentCount - 1)
            }
            return result
        } catch (e: Exception) {
            Log.e("CommentManager", "Error unliking comment", e)
            return false
        } finally {
            setCommentLikeLoading(commentId, false)
        }
    }

    suspend fun registerComments(comments: List<Comment>) {
        val commentIds = comments.map { it.id }
        if (commentIds.isEmpty()) return

        val (likeCounts, userLikedComments) = commentRequests.getCommentLikesInfo(commentIds)

        _likedComments.value = commentIds.associateWith { userLikedComments.contains(it) }
        _likeCounts.value = commentIds.associateWith { likeCounts[it] ?: 0 }
    }

    companion object {
        @Volatile
        private var INSTANCE: CommentInteractionManager? = null

        fun getInstance(api: BreezyAPI): CommentInteractionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommentInteractionManager(api).also { INSTANCE = it }
            }
        }
    }
}