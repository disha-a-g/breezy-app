package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.Channel
import com.whirlyfan.breezy.components.ChannelRow
import com.whirlyfan.breezy.viewmodels.ChannelViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: ChannelViewModel = viewModel(
        factory = ChannelViewModel.Factory(LocalBreezyAPI.current)
    )
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUser.collectAsStateWithLifecycle()

    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    // Handle refresh using coroutineScope
    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            viewModel.refreshChannels()
        }
    }

    // Update refresh state based on loading state
    LaunchedEffect(isLoading) {
        if (!isLoading && isRefreshing) {
            isRefreshing = false
        }
    }

    // Dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Filter channels based on search query
    val filteredChannels = remember(channels, searchQuery) {
        val filtered = if (searchQuery.isEmpty()) {
            channels
        } else {
            channels.filter { channelWithMembers ->
                val displayName = viewModel.getDisplayName(
                    channelWithMembers.channel,
                    channelWithMembers.members
                )
                displayName.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort by lastMessageAt (newest first), putting null timestamps last
        filtered.sortedWith(compareByDescending {
            it.channel.lastMessageAt ?: ""
        })
    }

    LaunchedEffect(Unit) {
        viewModel.refreshChannels()
    }

    Scaffold(
        topBar = {
            Column {
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
                        Text(text = "Channels")
                    }

                    IconButton(onClick = { navController.navigate("createChannel") }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Message"
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search channels") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && !isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Error: $error")
                }
            } else if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Create a channel to start chatting!"
                        else "No channels matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(filteredChannels) { channelWithMembers ->
                        ChannelRow(
                            channel = channelWithMembers.channel,
                            members = channelWithMembers.members,
                            currentUserId = currentUserId,
                            onLongClick = {
                                selectedChannel = channelWithMembers.channel
                                showDeleteDialog = true
                            },
                            onClick = {
                                val displayName = viewModel.getDisplayName(
                                    channelWithMembers.channel,
                                    channelWithMembers.members
                                )
                                val encodedName = java.net.URLEncoder.encode(displayName, "UTF-8")
                                navController.navigate("messages/${channelWithMembers.channel.id}/$encodedName")
                            }
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && selectedChannel != null) {
            val selectedChannelMembers =
                channels.firstOrNull { it.channel.id == selectedChannel?.id }?.members
                    ?: emptyList()
            val channelDisplayName =
                selectedChannel?.let { viewModel.getDisplayName(it, selectedChannelMembers) }
                    ?: "Unknown channel"

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Channel") },
                text = { Text("Are you sure you want to delete channel \"$channelDisplayName\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedChannel?.let {
                                viewModel.deleteChannel(it.id)
                                showDeleteDialog = false
                            }
                        }
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}