package com.whirlyfan.breezy.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.UserRequests
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

class MainViewModel(
    api: BreezyAPI,
) : ViewModel() {
    private val auth = api.client.auth
    private val userRequests: UserRequests = UserRequests(api)
    private val _isAuthenticated = mutableStateOf(false)
    val isAuthenticated: State<Boolean> = _isAuthenticated
    private val _hasUsername = mutableStateOf(false)
    val hasUsername: State<Boolean> = _hasUsername

    private val _isInitializing = mutableStateOf(true)
    val isInitializing: State<Boolean> = _isInitializing

    private fun setAuthenticated(authenticated: Boolean) {
        _isAuthenticated.value = authenticated
        if (!authenticated) {
            _hasUsername.value = false
        }
    }

    fun checkUserProfile() {
        viewModelScope.launch {
            val user = userRequests.getUser()
            _hasUsername.value = !user?.username.isNullOrBlank()
            _isInitializing.value = false
        }
    }


    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        println("Received new authenticated session.")
                        setAuthenticated(true)
                        val userId = status.session.user?.id ?: ""
                        userRequests.setAuthenticatedUser(userId)
                        checkUserProfile()
                        when (status.source) {
                            SessionSource.External -> { // handle external session
                            }

                            is SessionSource.Refresh -> { // handle refresh session
                            }

                            is SessionSource.SignIn -> {
                                // Clear post cache when a new user signs in
                                PostViewModel.clearCache()
                                userRequests.clearCache()
                                checkUserProfile()
                            }

                            is SessionSource.SignUp -> { // handle sign up
                            }

                            SessionSource.Storage -> { // handle storage session
                            }

                            SessionSource.Unknown -> { // handle unknown source
                            }

                            is SessionSource.UserChanged -> { // handle user changed
                                PostViewModel.clearCache()
                                userRequests.clearCache()
                                checkUserProfile()
                            }

                            is SessionSource.UserIdentitiesChanged -> { // handle identities changed
                            }

                            SessionSource.AnonymousSignIn -> TODO()
                        }
                    }

                    SessionStatus.Initializing -> println("Initializing")
                    is SessionStatus.RefreshFailure -> {
                        println("Refresh failure ${status.cause}")
                        _isInitializing.value = false
                    }

                    is SessionStatus.NotAuthenticated -> {
                        setAuthenticated(false)
                        userRequests.setAuthenticatedUser(null)
                        _isInitializing.value = false
                        if (status.isSignOut) {
                            println("User signed out")
                        } else {
                            println("User not signed in")
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(
    private val api: BreezyAPI,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
