package com.whirlyfan.breezy.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.R
import com.whirlyfan.breezy.api.Post
import com.whirlyfan.breezy.formatTimestamp
import com.whirlyfan.breezy.viewmodels.PostInteractionManager
import com.whirlyfan.breezy.viewmodels.PostViewModel
import kotlin.toString

@Composable
fun PostRow(
    post: Post,
    viewModel: PostViewModel,
    onCommentClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    disableProfileClick: Boolean = false
) {
    val interactionManager = PostInteractionManager.getInstance(LocalBreezyAPI.current)
    val likedPosts by interactionManager.likedPosts.collectAsStateWithLifecycle()
    val likeCounts by interactionManager.likeCounts.collectAsStateWithLifecycle()
    val commentCounts by interactionManager.commentCounts.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }

    val isLiked = likedPosts[post.id] ?: post.isLiked
    val localLikeCount = likeCounts[post.id] ?: post.likesCount ?: 0
    val localCommentCount = commentCounts[post.id] ?: post.commentsCount ?: 0

    val imageLoading = remember { mutableStateOf(true) }
    val imageError = remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }

    val loadingLikePosts by interactionManager.loadingLikePosts.collectAsStateWithLifecycle()
    val isLikeLoading = loadingLikePosts.contains(post.id)

    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    val isPostOwner = remember(post.id, post.userId, currentUserId) {
        post.userId?.let { viewModel.isCurrentUserPostOwner(it) } == true
    }

    LaunchedEffect(Unit) {
        if (viewModel.currentUserId.value == null) {
            viewModel.fetchCurrentUserId()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // User avatar and username row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .padding(
                        start = 16.dp,
                        end = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .let {
                            if (disableProfileClick) it
                            else it.clickable { post.userId?.let { userId -> onProfileClick(userId) } }
                        }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(
                        username = post.username ?: "Unknown user",
                        presetSize = AvatarSize.Small
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = post.username ?: "Unknown user",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatTimestamp(post.createdAt.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // This spacer pushes the options button to the far right
                Spacer(modifier = Modifier.weight(1f))

                if (isPostOwner) {
                    // Options button - without additional padding
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Post options"
                            )
                        }

                        // Dropdown menu
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    viewModel.deletePost(post.id)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete post",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Image handling
            post.imageUrl?.let { imageUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Show loading indicator while image loads
                    if (imageLoading.value && !imageError.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    // Image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .listener(
                                onError = { _, result ->
                                    Log.e(
                                        "ImageLoading",
                                        "Error loading image: ${result.throwable.message}"
                                    )
                                    result.throwable.printStackTrace()
                                }
                            )
                            .build(),
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { imageLoading.value = true },
                        onSuccess = { imageLoading.value = false },
                        onError = {
                            imageLoading.value = false
                            imageError.value = true
                        }
                    )

                    // Show error icon if image failed to load
                    if (imageError.value) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.outline_broken_image_24),
                            contentDescription = "Image failed to load",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Content
            if (post.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val lineHeight = MaterialTheme.typography.bodyMedium.fontSize.value * 1.5f
                    val maxLines = 15
                    val maxHeight = (lineHeight * maxLines).dp

                    val scrollState = rememberScrollState()
                    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if ((textLayoutResult.value?.lineCount ?: 0) > maxLines) {
                                    Modifier
                                        .heightIn(max = maxHeight)
                                        .verticalScroll(scrollState)
                                } else {
                                    Modifier
                                }
                            ),
                        onTextLayout = { textLayoutResult.value = it }
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Like button with count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isLikeLoading) {
                                if (isLiked) {
                                    viewModel.unlikePost(post.id)
                                } else {
                                    viewModel.likePost(post.id)
                                }
                            }
                        },
                        enabled = !isLikeLoading
                    ) {
                        if (isLikeLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Text(
                        text = "$localLikeCount Likes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Comment button with count
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val commentIcon =
                        ImageVector.vectorResource(id = R.drawable.outline_mode_comment_24)

                    IconButton(onClick = {
                        showComments = true
                        onCommentClick()
                    }) {
                        Icon(
                            imageVector = commentIcon,
                            contentDescription = "Comment",
                        )
                    }

                    Text(
                        text = "$localCommentCount Comments",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Show comments bottom sheet when activated
        if (showComments) {
            CommentBottomSheet(
                post = post,
                onDismiss = { showComments = false }
            )
        }
    }
}