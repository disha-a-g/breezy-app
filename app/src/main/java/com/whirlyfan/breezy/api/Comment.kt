package com.whirlyfan.breezy.api

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.text.get

@Serializable
data class Comment(
    val id: String,
    val content: String,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String?,
    val users: UserData? = null
) {
    val username: String?
        get() = users?.username
}

@Serializable
data class UserData(
    val username: String?
)

class CommentRequests(
    private val api: BreezyAPI,
) {
    private val userRequests = UserRequests(api)

    suspend fun getCommentsForPost(postId: String): List<Comment> =
        withContext(Dispatchers.IO) {
            try {
                val columns = Columns.raw(
                    """
                *,
                users (
                    username
                )
            """.trimIndent()
                )

                api.client.from("comments")
                    .select(columns = columns) {
                        filter {
                            eq("post_id", postId)
                        }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<Comment>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun addComment(postId: String, content: String): Comment? =
        withContext(Dispatchers.IO) {
            try {
                val user = userRequests.getUser()
                if (user == null) {
                    throw Exception("User not logged in")
                }

                val response = api.client.from("comments")
                    .insert(
                        CommentData(
                            content = content,
                            postId = postId,
                            userId = user.id
                        )
                    ) {
                        select()
                    }
                    .decodeList<Comment>()

                response.firstOrNull()
            } catch (e: Exception) {
                throw e
            }
        }

    suspend fun deleteComment(commentId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val user = userRequests.getUser()
                if (user == null) {
                    throw Exception("User not logged in")
                }

                // Delete the comment where id matches and user_id matches the current user
                // This ensures users can only delete their own comments
                api.client.from("comments")
                    .delete {
                        filter {
                            eq("id", commentId)
                            eq("user_id", user.id) // Security: only delete your own comments
                        }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    @Serializable
    data class CommentData(
        val content: String,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String
    )

    suspend fun likeComment(commentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRequests.getUser() ?: return@withContext false

            // Check if already liked
            val existingLike = api.client.from("comment_likes")
                .select {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", currentUser.id)
                    }
                }
                .decodeList<Map<String, String>>()

            if (existingLike.isNotEmpty()) {
                return@withContext true // Already liked
            }

            // Create new like
            api.client.from("comment_likes")
                .insert(
                    mapOf(
                        "comment_id" to commentId,
                        "user_id" to currentUser.id
                    )
                )
            true
        } catch (e: Exception) {
            Log.e("CommentRequests", "Error liking comment", e)
            false
        }
    }

    suspend fun unlikeComment(commentId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = userRequests.getUser() ?: return@withContext false

            // Delete the like
            api.client.from("comment_likes")
                .delete {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", currentUser.id)
                    }
                }
            true
        } catch (e: Exception) {
            Log.e("CommentRequests", "Error unliking comment", e)
            false
        }
    }

    suspend fun getCommentLikesInfo(commentIds: List<String>): Pair<Map<String, Int>, Set<String>> =
        withContext(Dispatchers.IO) {
            try {
                if (commentIds.isEmpty()) return@withContext Pair(emptyMap(), emptySet())
                val currentUser = userRequests.getUser()

                // Get all likes for these comments
                val allLikes = api.client.from("comment_likes")
                    .select {
                        filter { isIn("comment_id", commentIds) }
                    }
                    .decodeList<Map<String, String>>()

                // Calculate counts and user liked comments - fixed to handle nullable keys
                val likeCounts = allLikes
                    .mapNotNull { it["comment_id"] } // Filter out null keys
                    .groupingBy { it }
                    .eachCount() // Count occurrences directly

                val userLikedComments = if (currentUser != null) {
                    allLikes
                        .filter { it["user_id"] == currentUser.id }
                        .mapNotNull { it["comment_id"] } // Filter out null keys
                        .toSet()
                } else {
                    emptySet()
                }

                Pair(likeCounts, userLikedComments)
            } catch (e: Exception) {
                Log.e("CommentRequests", "Error fetching comment likes", e)
                Pair(emptyMap(), emptySet())
            }
        }
}