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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navController: NavController
) {
    val tabs = listOf("Following", "For You")

    val pagerState = rememberPagerState(initialPage = 1) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // Update ViewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // App title on the left
                Text(
                    text = "Breezy",
                    style = MaterialTheme.typography.headlineMedium,
                )

                // Tabs in the middle
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, title ->
                        val tabColor =
                            if (pagerState.currentPage == index) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        Column(
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = title,
                                color = tabColor,
                            )
                            if (pagerState.currentPage == index) {
                                Canvas(
                                    modifier =
                                        Modifier
                                            .padding(top = 4.dp)
                                            .size(width = 24.dp, height = 2.dp),
                                ) {
                                    drawRect(color = tabColor)
                                }
                            }
                        }
                    }
                }
                // Channel icon on the right
                IconButton(onClick = {
                    navController.navigate("channel")
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Channel",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Content Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> PostScreen(filter = "following", navController)
                    1 -> PostScreen(filter = "forYou", navController)
                }
            }
        }
    }
}
