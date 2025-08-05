package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.components.SearchResultsPane
import com.whirlyfan.breezy.components.UserResultsPane
import com.whirlyfan.breezy.viewmodels.PostViewModel
import com.whirlyfan.breezy.viewmodels.SearchViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(LocalBreezyAPI.current)
    ),
) {
    val breezyAPI = LocalBreezyAPI.current
    val postViewModel: PostViewModel = viewModel(
        factory = PostViewModel.Factory(breezyAPI)
    )

    val tabs = listOf("Posts", "Users")
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = currentTab) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // Update ViewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentTab(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    tabs.forEachIndexed { index, title ->
                        val tabColor = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }

                        Column(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                color = tabColor
                            )
                            if (pagerState.currentPage == index) {
                                Canvas(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(width = 24.dp, height = 2.dp)
                                ) {
                                    drawRect(color = tabColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> SearchResultsPane(
                    isLoading = viewModel.isPostsLoading.collectAsStateWithLifecycle().value,
                    error = viewModel.postsError.collectAsStateWithLifecycle().value,
                    posts = viewModel.posts.collectAsStateWithLifecycle().value,
                    viewModel = postViewModel,
                    searchQuery = searchQuery,
                    navController = navController
                )

                1 -> UserResultsPane(
                    isLoading = viewModel.isUsersLoading.collectAsStateWithLifecycle().value,
                    error = viewModel.usersError.collectAsStateWithLifecycle().value,
                    users = viewModel.users.collectAsStateWithLifecycle().value,
                    navController = navController,
                    searchQuery = searchQuery
                )
            }
        }
    }
}