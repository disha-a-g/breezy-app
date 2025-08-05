package com.whirlyfan.breezy.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserInner(
    val id: String,
    val username: String,
    // @SerialName("profile_url") val profileUrl: String? = null
)

@Serializable
data class PostInner(
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class LikeRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("post_id")    val postId:    String,
    val users: UserInner,
    val posts: PostInner? = null
)

@Serializable
data class CommentRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("post_id") val postId: String,
    val content: String,
    val users: UserInner,
    val posts: PostInner? = null
)

@Serializable
data class FollowRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    val users: UserInner
)

@Serializable
data class CommentLikeRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("comment_id") val commentId: String,
    val users: UserInner,
    val comments: CommentInner
)

@Serializable
data class CommentInner(
    val content: String,
    val posts: PostInner? = null
)

class ActivityRequests(
    private val client: SupabaseClient
) {
    suspend fun fetchMyPostIds(currentUserId: String): List<String> = withContext(Dispatchers.IO) {
        client
            .from("posts")
            .select(columns = Columns.list("id")) {
                filter { eq("user_id", currentUserId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<Map<String,String>>()
            .mapNotNull { it["id"] }
    }

    suspend fun fetchLikes(postIds: List<String>): List<LikeRow> = withContext(Dispatchers.IO) {
        client
            .from("post_likes")
            .select(columns = Columns.raw("""
                id,
                created_at,
                post_id,
                users:post_likes_user_id_fkey(id,username),
                posts:post_likes_post_id_fkey(image_url)
                """.trimIndent())) {
                filter { isIn("post_id", postIds) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<LikeRow>()
    }

    suspend fun fetchComments(postIds: List<String>): List<CommentRow> = withContext(Dispatchers.IO) {
        client
            .from("comments")
            .select(columns = Columns.raw("""
                id,
                created_at,
                post_id,
                content,
                users:comments_user_id_fkey(id,username),
                posts:comments_post_id_fkey(image_url)
                """.trimIndent())) {
                filter { isIn("post_id", postIds) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<CommentRow>()
    }

    suspend fun fetchFollows(currentUserId: String): List<FollowRow> = withContext(Dispatchers.IO) {
        client
            .from("followers")
            .select(columns = Columns.raw("""
        id,
        created_at,
        users:followers_user_id_fkey(id, username)
      """.trimIndent())) {
                filter { eq("following_id", currentUserId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<FollowRow>()
    }

    suspend fun fetchMyCommentIds(currentUserId: String): List<String> = withContext(Dispatchers.IO) {
        client
            .from("comments")
            .select(columns = Columns.list("id")) {
                filter { eq("user_id", currentUserId) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<Map<String, String>>()
            .mapNotNull { it["id"] }
    }

    suspend fun fetchCommentLikes(commentIds: List<String>): List<CommentLikeRow> = withContext(Dispatchers.IO) {
        client
            .from("comment_likes")
            .select(columns = Columns.raw("""
                id,
                created_at,
                comment_id,
                users:comment_likes_user_id_fkey(id,username),
                comments:comment_likes_comment_id_fkey(
                content,
                posts:comments_post_id_fkey(image_url)
                )
                """.trimIndent())) {
                filter { isIn("comment_id", commentIds) }
                order("created_at", order = Order.DESCENDING)
            }
            .decodeList<CommentLikeRow>()
    }

}