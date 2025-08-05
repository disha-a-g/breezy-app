package com.whirlyfan.breezy.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.whirlyfan.breezy.api.Message
import com.whirlyfan.breezy.formatTimestamp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    isFromCurrentUser: Boolean,
    modifier: Modifier = Modifier,
    onUnsendMessage: (String) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val messageColor = if (isFromCurrentUser) {
        colorScheme.primaryContainer
    } else {
        colorScheme.secondaryContainer
    }
    val textColor = if (isFromCurrentUser) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onSecondaryContainer
    }
    val alignment = if (isFromCurrentUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isFromCurrentUser && message.username != null) {
            Text(
                text = message.username,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isFromCurrentUser) 12.dp else 0.dp,
                            bottomEnd = if (isFromCurrentUser) 0.dp else 12.dp
                        )
                    )
                    .background(messageColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showDropdown = true }
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = textColor
                    )

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },

                        ) {
                        if (isFromCurrentUser) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            4.dp,
                                            Alignment.CenterHorizontally
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete message",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Unsend",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    onUnsendMessage(message.id)
                                    showDropdown = false
                                },
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = formatTimestamp(message.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}