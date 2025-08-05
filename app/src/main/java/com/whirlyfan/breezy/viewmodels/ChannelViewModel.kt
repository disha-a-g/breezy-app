package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Channel
import com.whirlyfan.breezy.api.ChannelRequests
import com.whirlyfan.breezy.api.ChannelUser
import com.whirlyfan.breezy.api.ChannelWithMembers
import com.whirlyfan.breezy.api.User
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelViewModel(
    private val channelRequests: ChannelRequests,
    private val userRequests: UserRequests
) : ViewModel() {
    private val _channels = MutableStateFlow<List<ChannelWithMembers>>(emptyList())
    val channels: StateFlow<List<ChannelWithMembers>> = _channels.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _followers = MutableStateFlow<List<User>>(emptyList())
    val followers: StateFlow<List<User>> = _followers.asStateFlow()

    private val _filteredFollowers = MutableStateFlow<List<User>>(emptyList())
    val filteredFollowers: StateFlow<List<User>> = _filteredFollowers.asStateFlow()

    private val _isLoadingFollowers = MutableStateFlow(false)
    val isLoadingFollowers: StateFlow<Boolean> = _isLoadingFollowers.asStateFlow()

    private val _followerError = MutableStateFlow<String?>(null)
    val followerError: StateFlow<String?> = _followerError.asStateFlow()


    init {
        loadChannels()
        loadCurrentUser()
    }

    fun refreshChannels() {
        loadChannels()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRequests.getUser()
                _currentUser.value = user?.id

                // Now that we have the user ID, load followers
                loadFollowers()
            } catch (e: Exception) {
                _error.value = "Failed to load user: ${e.message}"
            }
        }
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _channels.value = channelRequests.getUserChannels()
            } catch (e: Exception) {
                _error.value = "Failed to load channels: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFollowers() {
        viewModelScope.launch {
            _isLoadingFollowers.value = true
            _followerError.value = null
            try {
                val userId = _currentUser.value
                if (!userId.isNullOrEmpty()) {
                    _followers.value = userRequests.getFollowers(userId)
                    _filteredFollowers.value = _followers.value
                }
            } catch (e: Exception) {
                _followerError.value = "Failed to load followers: ${e.message}"
            } finally {
                _isLoadingFollowers.value = false
            }
        }
    }

    // Filter followers based on search query
    fun filterFollowers(query: String) {
        val trimmedQuery = query.trim()
        _filteredFollowers.value = if (trimmedQuery.isEmpty()) {
            _followers.value
        } else {
            _followers.value.filter { user ->
                user.username?.contains(trimmedQuery, ignoreCase = true) == true ||
                        user.bio?.contains(trimmedQuery, ignoreCase = true) == true
            }
        }
    }

    fun getDisplayName(channel: Channel, members: List<ChannelUser>): String {
        return if (!channel.name.isNullOrBlank()) {
            channel.name
        } else {
            when {
                members.size == 2 -> {
                    // For 2 people chats, show the other person's name
                    members.firstOrNull { it.id != _currentUser.value }?.username ?: "Unknown user"
                }

                members.size > 2 -> {
                    // For group chats, exclude the current user's name
                    val filteredMembers = members.filter { it.id != _currentUser.value }
                    val combinedNames =
                        filteredMembers.joinToString(", ") { it.username ?: "Unknown" }
                    if (combinedNames.length > 30) {
                        combinedNames.take(27) + "..."
                    } else {
                        combinedNames
                    }
                }

                else -> "Unknown chat"
            }
        }
    }

    fun deleteChannel(channelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = channelRequests.deleteChannel(channelId)
                if (success) {
                    // Remove the deleted channel from the list
                    _channels.value = _channels.value.filter {
                        it.channel.id != channelId
                    }
                } else {
                    _error.value = "Failed to delete channel"
                }
            } catch (e: Exception) {
                _error.value = "Error deleting channel: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val api: BreezyAPI) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChannelViewModel::class.java)) {
                return ChannelViewModel(
                    ChannelRequests(api),
                    UserRequests(api)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}