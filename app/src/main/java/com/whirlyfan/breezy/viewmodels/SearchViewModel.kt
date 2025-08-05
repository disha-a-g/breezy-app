package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Post
import com.whirlyfan.breezy.api.PostRequests
import com.whirlyfan.breezy.api.User
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val postRequests: PostRequests,
    private val userRequests: UserRequests
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isPostsLoading = MutableStateFlow(false)
    val isPostsLoading: StateFlow<Boolean> = _isPostsLoading.asStateFlow()

    private val _postsError = MutableStateFlow<String?>(null)
    val postsError: StateFlow<String?> = _postsError.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isUsersLoading = MutableStateFlow(false)
    val isUsersLoading: StateFlow<Boolean> = _isUsersLoading.asStateFlow()

    private val _usersError = MutableStateFlow<String?>(null)
    val usersError: StateFlow<String?> = _usersError.asStateFlow()

    // Track last queries for each tab to prevent duplicate searches
    private var lastPostsQuery: String? = null
    private var lastUsersQuery: String? = null

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300) // Debounce typing
                .collect { query ->
                    if (query.isNotBlank()) {
                        if (currentTab.value == 0) {
                            searchPosts(query)
                        } else {
                            searchUsers(query)
                        }
                    } else {
                        _posts.value = emptyList()
                        _users.value = emptyList()
                        // Reset last queries when search is cleared
                        lastPostsQuery = null
                        lastUsersQuery = null
                    }
                }
        }

        viewModelScope.launch {
            currentTab.collect { tab ->
                val query = searchQuery.value
                if (query.isNotBlank()) {
                    if (tab == 0) {
                        searchPosts(query)
                    } else {
                        searchUsers(query)
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
    }

    private fun searchPosts(query: String) {
        // Skip search if already performed with same query
        if (query == lastPostsQuery && posts.value.isNotEmpty()) return

        viewModelScope.launch {
            _isPostsLoading.value = true
            _postsError.value = null
            try {
                _posts.value = postRequests.searchPosts(query)
                lastPostsQuery = query
            } catch (e: Exception) {
                _postsError.value = "Failed to search posts: ${e.message}"
            } finally {
                _isPostsLoading.value = false
            }
        }
    }

    private fun searchUsers(query: String) {
        // Skip search if already performed with same query
        if (query == lastUsersQuery && users.value.isNotEmpty()) return

        viewModelScope.launch {
            _isUsersLoading.value = true
            _usersError.value = null
            try {
                _users.value = userRequests.searchUsers(query)
                lastUsersQuery = query
            } catch (e: Exception) {
                _usersError.value = "Failed to search users: ${e.message}"
            } finally {
                _isUsersLoading.value = false
            }
        }
    }

    class Factory(private val api: BreezyAPI) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(
                    PostRequests(api),
                    UserRequests(api)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}