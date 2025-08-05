package com.whirlyfan.breezy.components

import com.whirlyfan.breezy.api.LikeRow
import com.whirlyfan.breezy.api.CommentRow
import com.whirlyfan.breezy.api.FollowRow
import com.whirlyfan.breezy.api.CommentLikeRow
import com.whirlyfan.breezy.api.CommentInner
import java.time.Instant

sealed class Notification {
    abstract val id: String
    abstract val actorUsername: String
    abstract val actorId: String
    // abstract val actorProfileUrl: String?
    abstract val timestamp: Instant

    data class Liked(
        override val id: String,
        override val actorUsername: String,
        override val actorId: String,
        // override val actorProfileUrl: String?,
        val postId: String,
        val postThumbnailUrl: String?,
        override val timestamp: Instant
    ) : Notification()

    data class Commented(
        override val id: String,
        override val actorUsername: String,
        override val actorId: String,
        // override val actorProfileUrl: String?,
        val postId: String,
        val comment: String,
        val postThumbnailUrl: String?,
        override val timestamp: Instant
    ) : Notification()

    data class Followed(
        override val id: String,
        override val actorUsername: String,
        override val actorId: String,
        // override val actorProfileUrl: String?,
        override val timestamp: Instant
    ) : Notification()

    data class CommentLiked(
        override val id: String,
        override val actorUsername: String,
        override val actorId: String,
        val commentId: String,
        val commentText: String,
        val postThumbnailUrl: String?,
        override val timestamp: Instant
    ) : Notification()
}

fun parseNotifications(
    likes: List<LikeRow>,
    comments: List<CommentRow>,
    follows: List<FollowRow>,
    commentLikes: List<CommentLikeRow>
): List<Notification> {
    val all = mutableListOf<Notification>()

    likes.forEach {
        val ts = runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.now()
        all += Notification.Liked(
            id = it.id,
            actorUsername = it.users.username,
            actorId = it.users.id,
            postId = it.postId,
            postThumbnailUrl = it.posts?.imageUrl,
            timestamp = ts
        )
    }

    comments.forEach {
        val ts = runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.now()
        all += Notification.Commented(
            id = it.id,
            actorUsername = it.users.username,
            actorId = it.users.id,
            postId = it.postId,
            comment = it.content,
            postThumbnailUrl = it.posts?.imageUrl,
            timestamp = ts
        )
    }

    follows.forEach {
        val ts = runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.now()
        all += Notification.Followed(
            id = it.id,
            actorUsername = it.users.username,
            actorId = it.users.id,
            timestamp = ts
        )
    }

    commentLikes.forEach {
        val ts = runCatching { Instant.parse(it.createdAt) }.getOrNull() ?: Instant.now()
        all += Notification.CommentLiked(
            id = it.id,
            actorUsername = it.users.username,
            actorId = it.users.id,
            commentId = it.commentId,
            commentText = it.comments.content,
            postThumbnailUrl = it.comments.posts?.imageUrl,
            timestamp = ts
        )
    }

    return all.sortedByDescending { it.timestamp }
}