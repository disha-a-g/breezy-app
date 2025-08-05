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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.whirlyfan.breezy.LocalBreezyAPI
import com.whirlyfan.breezy.LocalMainViewModel
import com.whirlyfan.breezy.viewmodels.AuthViewModel
import com.whirlyfan.breezy.viewmodels.AuthViewModelFactory

@Composable
fun SignupProfileScreen(
    viewModel: AuthViewModel =
        viewModel(factory = AuthViewModelFactory(api = LocalBreezyAPI.current)),
) {
    val mainViewModel = LocalMainViewModel.current
    var username by remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set Your Username")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.errorMessage.isNotEmpty()) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp),
            ) {
                items(listOf(viewModel.errorMessage)) { error ->
                    Text(text = error)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isNotBlank()) {
                    viewModel.updateUser(
                        username = username,
                        onSuccess = {
                            mainViewModel.checkUserProfile()
                        },
                    )
                } else {
                    viewModel.errorMessage = "Username cannot be empty"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Continue")
            }
        }
    }
}
