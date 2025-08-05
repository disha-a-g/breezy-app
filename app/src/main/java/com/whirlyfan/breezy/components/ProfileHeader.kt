package com.whirlyfan.breezy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whirlyfan.breezy.api.User

@Composable
fun ProfileHeader(
    user: User,
    postCount: Int,
    followerCount: Int,
    followingCount: Int,
    isCurrentUser: Boolean,
    isFollowing: Boolean,
    isFollowActionLoading: Boolean,
    isProfileLoading: Boolean = false,
    isMessageLoading: Boolean = false,
    onToggleFollow: () -> Unit,
    onEditProfile: () -> Unit,
    onMessage: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Row with avatar and counts
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Avatar(
                username = user.username ?: "Unknown User",
                presetSize = AvatarSize.Large,
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Counts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(count = postCount, label = "Posts")
                StatColumn(
                    count = followerCount,
                    label = "Followers",
                    onClick = onFollowersClick
                )
                StatColumn(
                    count = followingCount,
                    label = "Following",
                    onClick = onFollowingClick
                )
            }
        }

        // Username
        Text(
            text = user.username ?: "Anonymous",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )

        // Bio (if exists)
        if (!user.bio.isNullOrBlank()) {
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCurrentUser) {
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Edit Profile")
                }
            } else {
                Button(
                    onClick = onMessage,
                    modifier = Modifier.weight(1f),
                    enabled = !isMessageLoading
                ) {
                    if (isMessageLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Message")
                    }
                }

                FollowButton(
                    isFollowing = isFollowing,
                    isLoading = isFollowActionLoading,
                    onClick = onToggleFollow,
                    modifier = Modifier.weight(1f),
                    enabled = !isProfileLoading
                )
            }
        }
    }
}