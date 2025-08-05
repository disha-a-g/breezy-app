package com.whirlyfan.breezy.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whirlyfan.breezy.api.Channel
import com.whirlyfan.breezy.api.ChannelUser
import com.whirlyfan.breezy.api.ChannelWithMembers
import com.whirlyfan.breezy.formatShortTimeStamp
import com.whirlyfan.breezy.formatTimestamp
import kotlin.compareTo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelRow(
    channel: Channel,
    members: List<ChannelUser>,
    currentUserId: String?,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    // Determine display name based on channel name and members
    val displayName = if (!channel.name.isNullOrBlank()) {
        channel.name
    } else {
        when {
            members.size == 2 -> {
                // For 2 people chats, show the other person's name
                members.first { it.id != currentUserId }.username ?: "Unknown user"
            }

            members.size > 2 -> {
                // For group chats, exclude the current user's name
                val filteredMembers = members.filter { it.id != currentUserId }
                val combinedNames = filteredMembers.joinToString(", ") { it.username ?: "Unknown" }
                if (combinedNames.length > 30) {
                    combinedNames.take(27) + "..."
                } else {
                    combinedNames
                }
            }

            else -> "Unknown chat"
        }

    }

    // Determine avatar username
    val avatarUsername = if (!channel.name.isNullOrBlank()) {
        channel.name
    } else if (members.size >= 2) {
        members.firstOrNull { it.id != currentUserId }?.username ?: "?"
    } else {
        "?"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                username = avatarUsername,
                presetSize = AvatarSize.Medium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (channel.lastMessageAt != null) {
                        val content = if (!channel.lastMessageContent.isNullOrBlank()) {
                            val truncatedContent = if (channel.lastMessageContent.length > 20) {
                                channel.lastMessageContent.take(20) + "..."
                            } else {
                                channel.lastMessageContent
                            }
                            "$truncatedContent • "
                        } else {
                            ""
                        }
                        content + formatShortTimeStamp(channel.lastMessageAt)
                    } else {
                        "No messages yet"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}