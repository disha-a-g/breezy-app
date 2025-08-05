package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.components.Avatar
import com.whirlyfan.breezy.components.AvatarSize
import com.whirlyfan.breezy.components.Notification
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import com.whirlyfan.breezy.formatShortTimeStamp
import com.whirlyfan.breezy.viewmodels.ActivityViewModel
import com.whirlyfan.breezy.viewmodels.ActivityViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = viewModel(
        factory = ActivityViewModelFactory(LocalBreezyAPI.current)
    )
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading   = uiState.isLoading
    val errorMessage= uiState.errorMessage
    val notifications = uiState.notifications

    var isRefreshing by remember { mutableStateOf(false) }
    val pullState    = rememberPullToRefreshState()
    val scope        = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch { viewModel.refresh() }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && isRefreshing) {
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Activity", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh     = onRefresh,
            state         = pullState,
            modifier      = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && !isRefreshing && notifications.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    }
                }
                notifications.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No Activity Yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationRow(item)
                        }
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "End of Activity List",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun NotificationRow(
    item: Notification,
) {
    val maxCommentLen = 40
    fun String.truncate() =
        if (length > maxCommentLen) take(maxCommentLen).trimEnd() + "..." else this

    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            username = item.actorUsername,
            presetSize = AvatarSize.Medium
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val description = when (item) {
                is Notification.Liked -> "${item.actorUsername} liked your post"
                is Notification.Commented -> "${item.actorUsername} commented: ${item.comment.truncate()}"
                is Notification.Followed -> "${item.actorUsername} started following you"
                is Notification.CommentLiked -> "${item.actorUsername} liked your comment: ${item.commentText.truncate()}"
            }
            val annotated = buildAnnotatedString {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(
                    when (item) {
                        is Notification.Liked -> item.actorUsername
                        is Notification.Commented -> item.actorUsername
                        is Notification.Followed -> item.actorUsername
                        is Notification.CommentLiked -> item.actorUsername
                    }
                )
                pop()
                append(description.removePrefix(item.actorUsername))
            }

            Text(
                annotated,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatShortTimeStamp(item.timestamp.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        val thumb = when (item) {
            is Notification.Liked -> item.postThumbnailUrl
            is Notification.Commented -> item.postThumbnailUrl
            is Notification.CommentLiked -> item.postThumbnailUrl
            else -> null
        }
        thumb?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        }
    }
}
