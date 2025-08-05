package com.whirlyfan.breezy.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.Comment
import com.whirlyfan.breezy.formatTimestamp
import com.whirlyfan.breezy.viewmodels.CommentInteractionManager
import com.whirlyfan.breezy.viewmodels.CommentViewModel
import kotlin.text.get
import kotlin.toString

@Composable
fun CommentRow(
    comment: Comment,
    viewModel: CommentViewModel,
    currentUserId: String?,
    onRequestDelete: (Comment) -> Unit
) {
    val isCurrentUserComment = comment.userId == currentUserId
    val commentInteractionManager = CommentInteractionManager.getInstance(LocalBreezyAPI.current)
    val likedComments by commentInteractionManager.likedComments.collectAsStateWithLifecycle()
    val likeCounts by commentInteractionManager.likeCounts.collectAsStateWithLifecycle()
    val loadingLikeComments by commentInteractionManager.loadingLikeComments.collectAsStateWithLifecycle()

    val isLiked = likedComments[comment.id] == true
    val likeCount = likeCounts[comment.id] ?: 0
    val isLikeLoading = loadingLikeComments.contains(comment.id)

    val rowModifier = if (isCurrentUserComment) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onRequestDelete(comment) }
                )
            }
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            username = comment.username ?: "Unknown user",
            presetSize = AvatarSize.Small,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Comment content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.username ?: "Unknown user",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTimestamp(comment.createdAt ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Like button and count section remains unchanged
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!isLikeLoading) {
                        if (isLiked) {
                            viewModel.unlikeComment(comment.id)
                        } else {
                            viewModel.likeComment(comment.id)
                        }
                    }
                },
                enabled = !isLikeLoading
            ) {
                if (isLikeLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike Comment" else "Like Comment",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "$likeCount Likes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}