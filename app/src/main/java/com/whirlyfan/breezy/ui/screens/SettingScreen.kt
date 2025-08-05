package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.viewmodels.AuthViewModel
import com.whirlyfan.breezy.viewmodels.AuthViewModelFactory

@Composable
fun SettingsScreen(navController: NavController) {
    val breezyAPI = LocalBreezyAPI.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(api = breezyAPI))

    Scaffold(
        topBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                // Center the title by giving the Text a weight.
                Text(
                    text = "Settings",
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logout button
            Button(
                onClick = {
                    authViewModel.logout {
                        // Handle successful logout
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
            ) {
                Text(text = "Logout")
            }
        }
    }
}
