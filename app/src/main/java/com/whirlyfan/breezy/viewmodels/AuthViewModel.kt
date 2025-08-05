package com.whirlyfan.breezy.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.UserRequests
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class AuthViewModel(
    private val api: BreezyAPI,
) : ViewModel() {
    private val userRequests = UserRequests(api)
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun login(onSuccess: () -> Unit) {
        isLoading = true
        viewModelScope.launch {
            try {
                // Using Supabase sign in function (update with your actual API call)
                api.client.auth.signInWith(Email) {
                    email = this@AuthViewModel.email
                    password = this@AuthViewModel.password
                }
                // Check response and call onSuccess if authentication is successful
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Login error"
            } finally {
                isLoading = false
            }
        }
    }

    fun signup(onSuccess: () -> Unit) {
        isLoading = true
        viewModelScope.launch {
            try {
                // Using Supabase sign up function (update with your actual API call)
                api.client.auth.signUpWith(Email) {
                    email = this@AuthViewModel.email
                    password = this@AuthViewModel.password
                }
                // Check response and call onSuccess if registration is successful
                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Signup error"
            } finally {
                isLoading = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        isLoading = true
        viewModelScope.launch {
            try {
                // Clear the post cache
                PostViewModel.clearCache()

                // Logout from Supabase
                api.client.auth.signOut()

                // Reset UserRequests cache
                userRequests.clearCache()

                onSuccess()
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Logout error"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateUser(
        username: String? = null,
        bio: String? = null,
        onSuccess: () -> Unit = {},
    ) {
        isLoading = true
        viewModelScope.launch {
            try {
                userRequests.updateUser(username = username, bio = bio, onSuccess = onSuccess)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Update error"
            } finally {
                isLoading = false
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class AuthViewModelFactory(
    private val api: BreezyAPI,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
