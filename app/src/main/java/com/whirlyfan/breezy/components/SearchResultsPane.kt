package com.whirlyfan.breezy.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.whirlyfan.breezy.api.Post
import com.whirlyfan.breezy.viewmodels.PostViewModel

@Composable
fun SearchResultsPane(
    isLoading: Boolean,
    error: String?,
    posts: List<Post>,
    viewModel: PostViewModel,
    searchQuery: String,
    navController: NavController
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error)
                }
            }

            posts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "No posts found"
                        else "No posts matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn {
                    items(posts) { post ->
                        PostRow(post, viewModel, onProfileClick = { userId ->
                            navController.navigate("profile/$userId")
                        })
                    }
                }
            }
        }
    }
}