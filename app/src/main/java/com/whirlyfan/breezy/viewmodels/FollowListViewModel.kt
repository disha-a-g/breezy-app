package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.User
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FollowListViewModel(
    private val breezyAPI: BreezyAPI,
    private val userId: String,
    private val isFollowers: Boolean
) : ViewModel() {
    private val userRequests = UserRequests(breezyAPI)

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Filtered users based on search query
    val filteredUsers = combine(_users, _searchQuery) { users, query ->
        if (query.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.username?.contains(query, ignoreCase = true) == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadUsers()
    }

    fun refreshUsers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            try {
                _users.value = if (isFollowers) {
                    userRequests.getFollowing(userId)
                } else {
                    userRequests.getFollowers(userId)
                }
            } catch (e: Exception) {
                _error.value = "Failed to refresh users: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _users.value = if (isFollowers) {
                    userRequests.getFollowing(userId)
                } else {
                    userRequests.getFollowers(userId)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(
        private val breezyAPI: BreezyAPI,
        private val userId: String,
        private val isFollowers: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FollowListViewModel::class.java)) {
                return FollowListViewModel(breezyAPI, userId, isFollowers) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}