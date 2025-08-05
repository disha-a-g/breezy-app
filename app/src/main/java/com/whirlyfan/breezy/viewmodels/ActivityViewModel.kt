package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.ActivityRequests
import com.whirlyfan.breezy.api.UserRequests
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.components.Notification
import com.whirlyfan.breezy.components.parseNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

data class ActivityUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ActivityViewModel(
    private val api: BreezyAPI
) : ViewModel() {
    private val requests      = ActivityRequests(api.client)
    private val userRequests  = UserRequests(api)

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            userRequests.getUser()?.id?.let { id ->
                currentUserId = id
                refresh()
            } ?: run {
                _uiState.value = ActivityUiState(errorMessage = "Not signed in")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val postIds  = requests.fetchMyPostIds(currentUserId)
                val likes    = requests.fetchLikes(postIds)
                val comments = requests.fetchComments(postIds)
                val follows  = requests.fetchFollows(currentUserId)
                val myCommentIds = requests.fetchMyCommentIds(currentUserId)
                val commentLikes = requests.fetchCommentLikes(myCommentIds)

                val filteredLikes     = likes      .filter     { it.users.id    != currentUserId }
                val filteredComments  = comments   .filter     { it.users.id    != currentUserId }
                val filteredFollows   = follows    .filter     { it.users.id    != currentUserId }
                val filteredCmtLikes  = commentLikes.filter     { it.users.id    != currentUserId }

                val all = parseNotifications(
                    likes        = filteredLikes,
                    comments     = filteredComments,
                    follows      = filteredFollows,
                    commentLikes = filteredCmtLikes
                )

                _uiState.value = ActivityUiState(
                    notifications = all,
                    isLoading     = false,
                    errorMessage  = null
                )
            } catch (t: Throwable) {
                _uiState.value = ActivityUiState(
                    notifications = emptyList(),
                    isLoading     = false,
                    errorMessage  = t.localizedMessage ?: "Failed to load activity"
                )
            }
        }
    }
}

class ActivityViewModelFactory(
    private val api: BreezyAPI
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
