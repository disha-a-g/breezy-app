package com.whirlyfan.breezy.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CommentInput(
    onSendComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text("Add a comment...") },
            modifier = modifier.weight(1f),
            singleLine = true
        )

        IconButton(
            onClick = {
                if (commentText.isNotBlank()) {
                    onSendComment(commentText)
                    commentText = ""
                }
            }
        ) {
            Icon(

                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Comment"
            )
        }
    }
}