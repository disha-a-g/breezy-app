package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.components.MessageInput
import com.whirlyfan.breezy.components.MessagesList
import com.whirlyfan.breezy.viewmodels.MessageViewModel

@Composable
fun MessageScreen(
    channelId: String,
    displayName: String,
    onBackClick: () -> Unit,
    viewModel: MessageViewModel = viewModel(
        factory = MessageViewModel.Factory(LocalBreezyAPI.current, channelId, displayName)
    )
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val channelName by viewModel.channelName.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    // TODO: If notifications are going to be added, we would need to modify this and maintain an open subscription to listen for notifications
    // Add this lifecycle effect to handle subscription cleanup
    DisposableEffect(Unit) {
        // Subscribe when the composable enters composition
        viewModel.subscribeToMessages()

        // Unsubscribe when the composable leaves composition
        onDispose {
            viewModel.unsubscribeFromMessages()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                // TODO: This is the magic line that keeps the top bar on the screen when the keyboard is out, it's not perfect but it works. Ideally this should be fixed
                modifier = Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                MessageInput(
                    value = messageText,
                    onValueChange = { messageText = it },
                    onSend = {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                messages.isEmpty() -> {
                    Text(
                        text = "No messages yet. Start the conversation!",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    MessagesList(
                        messages = messages,
                        currentUserId = currentUserId,
                        onUnsendMessage = { messageId -> viewModel.unsendMessage(messageId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
