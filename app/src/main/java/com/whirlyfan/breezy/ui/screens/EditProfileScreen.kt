package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.parseSupabaseError
import com.whirlyfan.breezy.viewmodels.ProfileViewModel

@Composable
fun EditProfileScreen(navController: NavController) {
    val breezyAPI = LocalBreezyAPI.current
    val viewModel = viewModel<ProfileViewModel>(
        factory = ProfileViewModel.Factory(breezyAPI)
    )

    // Collect flows as state
    val user by viewModel.user.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    val updateSuccess by viewModel.updateSuccess.collectAsStateWithLifecycle()

    // UI state
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Initialize fields with user data when loaded
    LaunchedEffect(user) {
        user?.let {
            username = it.username ?: ""
            bio = it.bio ?: ""
        }
    }

    // Handle navigation after successful update
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            navController.popBackStack()
            viewModel.resetUpdateSuccess()
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
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorMessage?.contains("Username") == true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseSupabaseError(it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            viewModel.updateUser(
                                username = username,
                                bio = bio
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}