package com.whirlyfan.breezy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Comment
import com.whirlyfan.breezy.api.CommentRequests
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(
    private val api: BreezyAPI,
    private val commentRequests: CommentRequests,
    private val postId: String
) : ViewModel() {
    private val interactionManager = PostInteractionManager.getInstance(api)
    private val commentInteractionManager = CommentInteractionManager.getInstance(api)
    private val userRequests = UserRequests(api)
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentUser()
        loadComments()
    }

    private fun loadComments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fetchedComments = commentRequests.getCommentsForPost(postId)
                _comments.value = fetchedComments
                commentInteractionManager.registerComments(fetchedComments)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRequests.getUser()
                _currentUserId.value = user?.id
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun likeComment(commentId: String) {
        viewModelScope.launch {
            commentInteractionManager.likeComment(commentId)
        }
    }

    fun unlikeComment(commentId: String) {
        viewModelScope.launch {
            commentInteractionManager.unlikeComment(commentId)
        }
    }

    fun addComment(content: String) {
        viewModelScope.launch {
            try {
                commentRequests.addComment(postId, content)
                loadComments() // Refresh comments after adding
                interactionManager.incrementCommentCount(postId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                val success = commentRequests.deleteComment(commentId)
                if (success) {
                    loadComments() // Refresh comments after deletion
                    interactionManager.decrementCommentCount(postId)
                } else {
                    _error.value = "Failed to delete comment"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    class Factory(private val api: BreezyAPI, private val postId: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
                return CommentViewModel(api, CommentRequests(api), postId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}