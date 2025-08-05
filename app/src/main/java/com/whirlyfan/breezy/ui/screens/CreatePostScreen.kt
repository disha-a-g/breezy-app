package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.viewmodels.PostViewModel
import com.whirlyfan.breezy.viewmodels.PostViewModelFactory
import java.io.File
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun CreatePostScreen(
    navController: NavController,
    imageUriString: String
) {
    val imageUri = imageUriString.toUri()
    val context = LocalContext.current
    val breezyAPI = LocalBreezyAPI.current
    val postViewModel: PostViewModel = viewModel(factory = PostViewModelFactory(breezyAPI))

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(postViewModel.uploadSuccess) {
        if (postViewModel.uploadSuccess) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // Back button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back"
                    )
                }

                // Title
                Text(
                    text = "Create Post",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image with 1:1 aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Content input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // Post button
            Button(
                onClick = {
                    isLoading = true
                    // Check if it's a file URI or content URI
                    if (imageUri.scheme == "file") {
                        // For file URIs, convert to File object
                        val file = File(imageUri.path!!)
                        postViewModel.createPost(title = title, content = content, imageFile = file)
                    } else {
                        // For content URIs, use the context method
                        postViewModel.createPost(
                            title = title,
                            content = content,
                            imageUri = imageUri,
                            context = context
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = title.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Post")
                }
            }

            if (postViewModel.uploadError.isNotEmpty()) {
                Text(
                    text = postViewModel.uploadError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}