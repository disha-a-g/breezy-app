package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.ChannelRequests
import com.whirlyfan.breezy.components.PostRow
import com.whirlyfan.breezy.components.ProfileHeader
import com.whirlyfan.breezy.viewmodels.PostViewModel
import com.whirlyfan.breezy.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController, userId: String? = null) {
    // Initialize Breezy API and ViewModels here because we need to pass userId
    val breezyAPI = LocalBreezyAPI.current
    remember { ChannelRequests(breezyAPI) }
    rememberCoroutineScope()

    // Create the view model with appropriate userId
    val profileViewModel = viewModel<ProfileViewModel>(
        factory = ProfileViewModel.Factory(breezyAPI, userId)
    )

    // Track if we're returning from EditProfileScreen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reload user data when screen is resumed
                profileViewModel.loadUser()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Create post view model for the user's posts
    val postViewModel = viewModel<PostViewModel>(
        factory = PostViewModel.Factory(
            breezyAPI,
            if (userId == null) "profile" else "user_$userId"
        )
    )

    // Collect states
    val user by profileViewModel.user.collectAsStateWithLifecycle()
    val isCurrentUser by profileViewModel.isCurrentUser.collectAsStateWithLifecycle()
    val followerCount by profileViewModel.followerCount.collectAsStateWithLifecycle()
    val followingCount by profileViewModel.followingCount.collectAsStateWithLifecycle()
    val isProfileLoading by profileViewModel.isLoading.collectAsStateWithLifecycle()
    val posts by postViewModel.posts.collectAsStateWithLifecycle()
    val isLoading by postViewModel.isLoading.collectAsStateWithLifecycle()
    val error by postViewModel.error.collectAsStateWithLifecycle()
    val isFollowing by profileViewModel.isFollowing.collectAsStateWithLifecycle()
    val isFollowActionLoading by profileViewModel.isFollowActionLoading.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var isCreatingChannel by remember { mutableStateOf(false) }
    val postCount = posts.size

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = user?.username ?: "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (isCurrentUser) {
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profile header
            item {
                user?.let { userData ->
                    ProfileHeader(
                        user = userData,
                        postCount = postCount,
                        followerCount = followerCount,
                        followingCount = followingCount,
                        isCurrentUser = isCurrentUser,
                        isFollowing = isFollowing,
                        isFollowActionLoading = isFollowActionLoading,
                        isProfileLoading = isProfileLoading,
                        isMessageLoading = isCreatingChannel,
                        onToggleFollow = { profileViewModel.toggleFollow() },
                        onEditProfile = { navController.navigate("editProfile") },
                        onFollowersClick = {
                            userData.let {
                                navController.navigate("followers/${userData.id}")
                            }
                        },
                        onFollowingClick = {
                            userData.let {
                                navController.navigate("following/${userData.id}")
                            }
                        },
                        onMessage = {
                            if (!isCreatingChannel && user != null) {
                                isCreatingChannel = true
                                coroutineScope.launch {
                                    try {
                                        val channelRequests = ChannelRequests(breezyAPI)
                                        val channel =
                                            channelRequests.createDirectMessageChannel(userId!!)

                                        val encodedName = java.net.URLEncoder.encode(
                                            user?.username ?: "Unknown",
                                            "UTF-8"
                                        )
                                        navController.navigate("messages/${channel.id}/$encodedName")
                                    } catch (e: Exception) {
                                        // Handle error
                                    } finally {
                                        isCreatingChannel = false
                                    }
                                }
                            }
                        }
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Posts content
            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                error != null -> {
                    item {
                        Text(
                            text = error ?: "Error loading posts",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                posts.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No posts yet!\nTap the Camera to create one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    items(posts) { post ->
                        PostRow(
                            post, postViewModel, onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            }, disableProfileClick = isCurrentUser || userId == post.userId
                        )
                    }
                }
            }
        }
    }
}