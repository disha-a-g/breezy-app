package com.whirlyfan.breezy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.api.parseSupabaseError
import com.whirlyfan.breezy.viewmodels.AuthViewModel
import com.whirlyfan.breezy.viewmodels.AuthViewModelFactory

@Composable
fun LoginScreen(navController: NavController) {
    val breezyAPI = LocalBreezyAPI.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(api = breezyAPI))

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Breezy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = authViewModel.email,
            onValueChange = { authViewModel.email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = authViewModel.password,
            onValueChange = { authViewModel.password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // This is for debugging, we will replace it with a proper error message
        // We shouldn't display any error messages from the backend and instead create our own
        // error handling on the ui
        if (authViewModel.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = parseSupabaseError(authViewModel.errorMessage),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                authViewModel.login {
                    // Handle successful login
                }
            },
        ) {
            if (authViewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Login")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("signup") }) {
            Text(text = "Don\'t have an account? Sign Up")
        }
    }
}
