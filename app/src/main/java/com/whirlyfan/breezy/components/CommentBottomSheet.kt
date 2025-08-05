package com.whirlyfan.breezy.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.Comment
import com.whirlyfan.breezy.api.Post
import com.whirlyfan.breezy.viewmodels.CommentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    viewModel: CommentViewModel = viewModel(
        key = "comment_viewmodel_${post.id}",
        factory = CommentViewModel.Factory(LocalBreezyAPI.current, post.id)
    )
) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val focusRequester = remember { FocusRequester() }

    // Delete confirmation dialog - only shown if it's the current user's comment
    commentToDelete?.let { comment ->
        if (comment.userId == currentUserId) {
            AlertDialog(
                onDismissRequest = { commentToDelete = null },
                title = { Text("Delete Comment") },
                text = { Text("Are you sure you want to delete this comment?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteComment(comment.id)
                            commentToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { commentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Expand the sheet immediately when it appears
    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    // Request focus only when the sheet is fully expanded
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            focusRequester.requestFocus()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Comments list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = "Error loading comments: ${error ?: "Unknown error"}",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                comments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No comments yet. Be the first to comment!")
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(comments) { comment ->
                            CommentRow(
                                comment = comment,
                                viewModel = viewModel,
                                currentUserId = currentUserId,
                                onRequestDelete = { commentToDelete = it }
                            )
                        }
                    }
                }
            }

            // Comment input with focus requester to show keyboard
            CommentInput(
                onSendComment = { content ->
                    viewModel.addComment(content)
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }
}