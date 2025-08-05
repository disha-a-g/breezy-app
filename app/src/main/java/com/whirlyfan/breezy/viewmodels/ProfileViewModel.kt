package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.User
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val api: BreezyAPI, private val userId: String? = null) :
    ViewModel() {
    private val userRequests = UserRequests(api)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _isCurrentUser = MutableStateFlow(false)
    val isCurrentUser: StateFlow<Boolean> = _isCurrentUser.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _isFollowActionLoading = MutableStateFlow(false)
    val isFollowActionLoading: StateFlow<Boolean> = _isFollowActionLoading.asStateFlow()


    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userData = if (userId != null) {
                    userRequests.getUserById(userId)
                } else {
                    userRequests.getUser()
                }

                _user.value = userData

                // Determine if this is the current user's profile
                val currentUser = userRequests.getUser()
                _isCurrentUser.value = currentUser?.id == userData?.id

                // Load follower counts
                userData?.id?.let { id ->
                    _followerCount.value = userRequests.getFollowersCount(id)
                    _followingCount.value = userRequests.getFollowingCount(id)

                    // Check if current user is following this profile
                    if (!_isCurrentUser.value) {
                        _isFollowing.value = userRequests.isFollowing(id)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow() {
        if (userId == null || _isCurrentUser.value || _isFollowActionLoading.value) return

        viewModelScope.launch {
            _isFollowActionLoading.value = true
            try {
                // Use return value from follow/unfollow operations
                val success = if (_isFollowing.value) {
                    userRequests.unfollow(userId)
                } else {
                    userRequests.follow(userId)
                }

                if (success) {
                    // Only update UI state after confirmed success
                    if (_isFollowing.value) {
                        // We just unfollowed successfully
                        _isFollowing.value = false
                        _followerCount.value = _followerCount.value - 1
                    } else {
                        // We just followed successfully
                        _isFollowing.value = true
                        _followerCount.value = _followerCount.value + 1
                    }
                } else {
                    // Operation failed, refresh the actual state from server
                    _isFollowing.value = userRequests.isFollowing(userId)
                    _followerCount.value = userRequests.getFollowersCount(userId)
                    _error.value = "Failed to update following status"
                }
            } catch (e: Exception) {
                _error.value = "Failed to update following status: ${e.message}"
                // Refresh state on error
                _isFollowing.value = userRequests.isFollowing(userId)
                _followerCount.value = userRequests.getFollowersCount(userId)
            } finally {
                _isFollowActionLoading.value = false
            }
        }
    }


    fun updateUser(username: String, bio: String) {
        if (username.isBlank()) {
            _error.value = "Username cannot be empty"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            _updateSuccess.value = false

            try {
                userRequests.updateUser(username = username, bio = bio)

                // After successful update, reload the user data
                loadUser()

                // Signal success to the UI
                _updateSuccess.value = true
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    class Factory(private val api: BreezyAPI, private val userId: String? = null) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(api, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}