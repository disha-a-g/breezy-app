package com.whirlyfan.breezy.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AvatarSize {
    Small,
    Medium,
    Large
}

@Composable
fun Avatar(
    username: String,
    presetSize: AvatarSize? = null,
    size: Dp = when (presetSize) {
        AvatarSize.Small -> 32.dp
        AvatarSize.Medium -> 48.dp
        AvatarSize.Large -> 64.dp
        null -> 48.dp // Default size if no preset specified
    }
) {
    // Display the first character of the username as an avatar
    val username = username.firstOrNull()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (presetSize) {
            AvatarSize.Small -> Text(
                text = username,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodySmall
            )

            AvatarSize.Medium -> Text(
                text = username,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge
            )

            AvatarSize.Large -> Text(
                text = username,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium
            )

            null -> Text(
                text = username,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}