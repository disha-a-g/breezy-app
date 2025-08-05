package com.whirlyfan.breezy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.ui.screens.CameraScreen
import com.whirlyfan.breezy.ui.screens.ChannelScreen
import com.whirlyfan.breezy.ui.screens.CreateChannelScreen
import com.whirlyfan.breezy.ui.screens.EditProfileScreen
import com.whirlyfan.breezy.ui.screens.CreatePostScreen
import com.whirlyfan.breezy.ui.screens.FollowListScreen
import com.whirlyfan.breezy.ui.screens.HomeScreen
import com.whirlyfan.breezy.ui.screens.LoginScreen
import com.whirlyfan.breezy.ui.screens.MediaSelectionScreen
import com.whirlyfan.breezy.ui.screens.MessageScreen
import com.whirlyfan.breezy.ui.screens.ProfileScreen
import com.whirlyfan.breezy.ui.screens.SearchScreen
import com.whirlyfan.breezy.ui.screens.SettingsScreen
import com.whirlyfan.breezy.ui.screens.SignupProfileScreen
import com.whirlyfan.breezy.ui.screens.SignupScreen
import com.whirlyfan.breezy.ui.screens.ActivityScreen
import com.whirlyfan.breezy.ui.theme.BreezyTheme
import com.whirlyfan.breezy.viewmodels.MainViewModel
import com.whirlyfan.breezy.viewmodels.MainViewModelFactory

val LocalBreezyAPI = staticCompositionLocalOf<BreezyAPI> { error("No BreezyAPI provided.") }
val LocalMainViewModel =
    staticCompositionLocalOf<MainViewModel> {
        error("No MainViewModel provided.")
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainActivityScreen()
        }
    }
}

// This makes the MainViewModel globally accessible via LocalMainViewModel.current.
// E.g.,    val mainViewModel = LocalMainViewModel.current
//          val isLoadingLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
// .collectAsStateWithLifecycle() is used to observe the state in a lifecycle-aware manner.
@Composable
fun MainActivityScreen() {
    val context = LocalContext.current
    // Create a single shared BreezyAPI instance.
    val breezyAPI = BreezyAPI(context = context)
    // Create the MainViewModel with the shared instance.
    val mainViewModel = viewModel<MainViewModel>(factory = MainViewModelFactory(api = breezyAPI))
    CompositionLocalProvider(
        LocalBreezyAPI provides breezyAPI,
        LocalMainViewModel provides mainViewModel,
    ) {
        val systemDarkTheme = isSystemInDarkTheme()
        BreezyTheme(darkTheme = systemDarkTheme) {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val onBackClick: () -> Unit = { navController.popBackStack() }
    val showBottomBar = rememberSaveable { mutableStateOf(true) }
    // Choose the start destination based on authentication state.
    val mainViewModel: MainViewModel =
        viewModel(factory = MainViewModelFactory(LocalBreezyAPI.current))
    val isAuthenticated = mainViewModel.isAuthenticated.value
    val isInitializing = mainViewModel.isInitializing.value
    val hasUsername = mainViewModel.hasUsername.value
    // Track if NavHost has been composed
    val isNavHostReady = rememberSaveable { mutableStateOf(false) }

    // Handle auth state changes only after NavHost is ready
    LaunchedEffect(isAuthenticated, hasUsername, isNavHostReady.value) {
        if (!isNavHostReady.value || isInitializing) return@LaunchedEffect

        when {
            !isAuthenticated -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }

            !hasUsername -> {
                navController.navigate("signupProfile") {
                    popUpTo(0) { inclusive = true }
                }
            }

            else -> {
                // Only navigate to home if not already there
                if (navController.currentDestination?.route != "home") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // Auto-manage bottom bar visibility based on current destination
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            // Routes that should hide bottom bar
            val hideBottomBarRoutes = listOf(
                "login", "signup", "media", "camera", "createPost/.*", "messages/.*",
                "signupProfile", "editProfile", "settings", "createChannel"
            )

            // Check if current route should hide bottom bar
            val currentRoute = entry.destination.route ?: ""
            val shouldHideBottomBar = hideBottomBarRoutes.any { route ->
                currentRoute.matches(Regex(route))
            }
            showBottomBar.value = !shouldHideBottomBar
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar.value && !isInitializing) {
                BottomNavigationBar(navController)
            }
        },
    ) { innerPadding ->
        if (isInitializing) {
            // Show loading screen while initializing
            LoadingScreen()
        } else {
            NavHost(
                navController = navController,
                startDestination = when {
                    !isAuthenticated -> "login"
                    !hasUsername -> "signupProfile"
                    else -> "home"
                },
                modifier = Modifier.padding(innerPadding),
            ) {
                // Public routes
                composable("login") {
                    LoginScreen(navController)
                }
                composable("signup") {
                    SignupScreen(navController)
                }
                // Protected routes
                composable("home") {
                    HomeScreen(
                        navController = navController,
                    )
                }
                composable("media") {
                    MediaSelectionScreen(navController)
                }
                composable("camera") {
                    CameraScreen(navController)
                }
                composable("activity") {
                    ActivityScreen()
                }
                composable(
                    route = "createPost/{imageUri}",
                    arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                    CreatePostScreen(navController, imageUri)
                }
                composable("search") { SearchScreen(navController) }
                composable("profile") { ProfileScreen(navController, null) } // Current user profile
                composable(
                    route = "profile/{userId}",
                    arguments = listOf(
                        navArgument("userId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")
                    ProfileScreen(navController, userId)
                }
                composable(
                    route = "followers/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    FollowListScreen(
                        navController = navController,
                        userId = userId,
                        isFollowers = true
                    )
                }

                composable(
                    route = "following/{userId}",
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    FollowListScreen(
                        navController = navController,
                        userId = userId,
                        isFollowers = false
                    )
                }
                composable("editProfile") {
                    EditProfileScreen(navController)
                }
                composable("settings") { SettingsScreen(navController) }
                composable("channel") { ChannelScreen(navController, onBackClick) }
                composable("createChannel") { CreateChannelScreen(navController, onBackClick) }
                composable("signupProfile") {
                    SignupProfileScreen()
                }
                composable(
                    route = "messages/{channelId}/{displayName}",
                    arguments = listOf(
                        navArgument("channelId") { type = NavType.StringType },
                        navArgument("displayName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    val displayName = java.net.URLDecoder.decode(
                        backStackEntry.arguments?.getString("displayName") ?: "",
                        "UTF-8"
                    )
                    MessageScreen(
                        channelId = channelId,
                        displayName = displayName,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf("home", "search", "media", "activity", "profile")
    val cameraIcon = ImageVector.vectorResource(id = R.drawable.outline_camera_alt_24)
    val icons =
        listOf(
            Icons.Filled.Home,
            Icons.Outlined.Search,
            cameraIcon,
            Icons.Outlined.Notifications,
            Icons.Filled.Person,
        )
    val labels = listOf("Home", "Search", "Camera", "Activity", "Profile")

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEachIndexed { index, route ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = labels[index]) },
                label = { Text(labels[index]) },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when navigating back to a previously selected item
                            restoreState = true
                            // Clear the back stack if navigating to the home screen
                            if (route == "home") {
                                popUpTo("home") { inclusive = true }
                            }
                            if (route == "login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                },
            )
        }
    }
}


@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}