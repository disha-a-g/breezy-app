package com.whirlyfan.breezy.api

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID
import kotlin.text.get
import kotlin.text.toLong

@Serializable
data class Post(
    val id: String,
    val title: String,
    val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("likes_count") val likesCount: Long? = 0,
    @SerialName("comments_count") val commentsCount: Long? = 0,
    val isLiked: Boolean = false,
    val username: String? = null
)

// SQL Query for Get All Posts with Like and Comment Counts
// SELECT
//    p.id AS post_id,
//    p.title,
//    p.content,
//    COALESCE(likes.likes_count, 0) AS likes_count,
//    COALESCE(comments.comments_count, 0) AS comments_count
//FROM
//    public.posts p
//LEFT JOIN (
//    SELECT
//        post_id,
//        COUNT(*) AS likes_count
//    FROM
//        public.post_likes
//    GROUP BY
//        post_id
//) AS likes ON likes.post_id = p.id
//LEFT JOIN (
//    SELECT
//        post_id,
//        COUNT(*) AS comments_count
//    FROM
//        public.comments
//    GROUP BY
//        post_id
//) AS comments ON comments.post_id = p.id;

class PostRequests(
    private val api: BreezyAPI,
) {
    private val userRequests = UserRequests(api)

    suspend fun getPosts(filter: String = "forYou"): List<Post> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = userRequests.getUser()
                val userId = currentUser?.id
                val query = api.client.from("posts")

                // Apply different query conditions based on filter
                val posts = when {
                    // Pattern for viewing specific user's posts: "user_[userId]"
                    filter.startsWith("user_") -> {
                        // Extract userId from filter string
                        val userId = filter.substringAfter("user_")
                        query.select {
                            filter {
                                eq("user_id", userId)
                            }
                            order("created_at", Order.DESCENDING) // Newest first
                        }
                    }

                    filter == "profile" -> {
                        // Get posts created by the current user
                        val currentUser = userRequests.getUser()
                        if (currentUser != null) {
                            query.select {
                                filter {
                                    eq("user_id", currentUser.id)
                                }
                                order("created_at", Order.DESCENDING) // Newest first
                            }
                        } else {
                            query.select {
                                order("created_at", Order.DESCENDING)
                            }
                        }
                    }

                    filter == "following" -> {
                        // Get current user ID
                        val currentUser = userRequests.getUser()

                        // Get posts from users the current user follows
                        if (currentUser != null) {
                            // First query to get the list of following IDs
                            val followingIds =
                                api.client
                                    .from("followers")
                                    .select(Columns.raw("following_id")) {
                                        filter {
                                            eq("user_id", currentUser.id)
                                        }
                                        order("created_at", order = Order.DESCENDING)
                                    }.decodeList<Map<String, String>>()
                                    .map { it["following_id"] ?: "" }
                                    .filter { it.isNotEmpty() }

                            // Second query to get the posts from the following IDs
                            if (followingIds.isNotEmpty()) {
                                query.select {
                                    filter {
                                        isIn("user_id", followingIds)
                                    }
                                    order("created_at", Order.DESCENDING) // Newest first
                                }
                            } else {
                                // No followings, return empty list
                                return@withContext emptyList()
                            }
                        } else {
                            // Fallback if no user ID available
                            query.select {
                                order("created_at", Order.DESCENDING)
                            }
                        }
                    }

                    else -> {
                        // Default "forYou" - get all posts or apply other criteria
                        query.select {
                            order("created_at", order = Order.DESCENDING)
                        }
                    }
                }.decodeList<Post>()
                if (posts.isEmpty()) return@withContext emptyList()

                // TODO: This is still inefficient, as it fetches likes and comments in separate queries.
                // But it's substantially better than the previous implementation which fetched likes and comments for each post individually.

                // Extract all unique user IDs from posts
                val userIds = posts.map { it.userId }.distinct()

                // Fetch usernames for these user IDs in a single query
                val userMap = api.client.from("users")
                    .select(Columns.raw("id, username")) {
                        filter {
                            isIn("id", userIds)
                        }
                    }
                    .decodeList<Map<String, String>>()
                    .associateBy({ it["id"] ?: "" }, { it["username"] ?: "" })


                // Get all post IDs
                val postIds = posts.map { it.id }

                // If we have a logged in user, fetch their likes for these posts
                val userLikedPostIds = if (userId != null) {
                    api.client.from("post_likes")
                        .select {
                            filter {
                                isIn("post_id", postIds)
                                eq("user_id", userId)
                            }
                        }
                        .decodeList<Map<String, String>>()
                        .mapNotNull { it["post_id"] }
                        .toSet()
                } else {
                    emptySet()
                }

                // Fetch all likes for these posts in a single query
                val allLikes = api.client.from("post_likes")
                    .select {
                        filter { isIn("post_id", postIds) }
                    }
                    .decodeList<Map<String, String>>()

                // Fetch all comments for these posts in a single query
                val allComments = api.client.from("comments")
                    .select {
                        filter { isIn("post_id", postIds) }
                    }
                    .decodeList<Map<String, String>>()

                // Group likes and comments by post ID
                val likesCountMap =
                    allLikes.groupBy { it["post_id"] }.mapValues { it.value.size.toLong() }
                val commentsCountMap =
                    allComments.groupBy { it["post_id"] }.mapValues { it.value.size.toLong() }

                // Create new posts with counts and liked status
                val postsWithCounts = posts.map { post ->
                    post.copy(
                        likesCount = likesCountMap[post.id] ?: 0,
                        commentsCount = commentsCountMap[post.id] ?: 0,
                        isLiked = userLikedPostIds.contains(post.id),
                        username = userMap[post.userId]
                    )
                }
                postsWithCounts
            } catch (e: Exception) {
                Log.e("PostRequests", "Error fetching posts", e)
                emptyList()
            }
        }

    suspend fun createPost(imageFile: File, title: String, content: String): Post? =
        withContext(Dispatchers.IO) {
            try {
                // Generate a unique file name
                val fileName = "post_${UUID.randomUUID()}.jpg"

                // Convert File to ByteArray for upload
                val byteArray = imageFile.readBytes()

                // Upload image to Supabase
                val bucket = api.client.storage["post-images"]
                bucket.upload(path = fileName, data = byteArray)

                // Get public URL for the uploaded image
                val imageUrl = bucket.publicUrl(fileName)

                // Create post with image URL
                val response = api.client.postgrest["posts"].insert(
                    mapOf(
                        "title" to title,
                        "content" to content,
                        "image_url" to imageUrl
                    )
                ) {
                    select()
                }.decodeList<Post>()

                if (response.isEmpty()) {
                    null
                } else {
                    response.first()
                }
            } catch (e: Exception) {
                Log.e("PostRequests", "Error creating post", e)
                null
            }
        }

    suspend fun deletePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete the post
            api.client.from("posts")
                .delete {
                    filter {
                        eq("id", postId)
                    }
                }
            true
        } catch (e: Exception) {
            Log.e("PostRequests", "Error deleting post", e)
            false
        }
    }


    suspend fun searchPosts(query: String): List<Post> =
        withContext(Dispatchers.IO) {
            try {
                val posts = api.client.from("posts")
                    .select {
                        filter {
                            or {
                                ilike("title", "%$query%")
                                ilike("content", "%$query%")
                            }
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Post>()

                if (posts.isEmpty()) return@withContext emptyList()

                // Extract all unique user IDs from posts
                val userIds = posts.map { it.userId }.distinct()

                // Fetch usernames for these user IDs in a single query
                val userMap = api.client.from("users")
                    .select(Columns.raw("id, username")) {
                        filter {
                            isIn("id", userIds)
                        }
                    }
                    .decodeList<Map<String, String>>()
                    .associateBy({ it["id"] ?: "" }, { it["username"] ?: "" })

                // Create new posts with username information
                posts.map { post ->
                    post.copy(username = userMap[post.userId])
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun likePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRequests.getUser() ?: return@withContext false

            // Check if user already liked the post
            val existingLike = api.client.from("post_likes")
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", currentUser.id)
                    }
                }
                .decodeList<Post>()

            if (existingLike.isNotEmpty()) {
                return@withContext true // Already liked
            }

            // Create new like
            api.client.from("post_likes")
                .insert(
                    mapOf(
                        "post_id" to postId,
                        "user_id" to currentUser.id
                    )
                )

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun unlikePost(postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRequests.getUser() ?: return@withContext false

            // Delete the like
            api.client.from("post_likes")
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", currentUser.id)
                    }
                }

            true
        } catch (e: Exception) {
            false
        }
    }
}

