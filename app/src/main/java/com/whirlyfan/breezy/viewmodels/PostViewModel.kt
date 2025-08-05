package com.whirlyfan.breezy.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Post
import com.whirlyfan.breezy.api.PostRequests
import com.whirlyfan.breezy.api.UserRequests
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class PostViewModel(
    private val api: BreezyAPI,
    private val userRequests: UserRequests = UserRequests(api),
    private val postRequests: PostRequests,
    private val filter: String,
    private val likeManager: PostInteractionManager = PostInteractionManager.getInstance(api)
) : ViewModel() {
    // Keep track of which filters have been loaded and their associated posts
    companion object {
        // Store both loaded filters and their posts
        private val cachedPostsByFilter = mutableMapOf<String, List<Post>>()

        // This method to clears cache on logout
        fun clearCache() {
            cachedPostsByFilter.clear()
        }
    }

    // Make current user ID reactive with StateFlow
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId = _currentUserId.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(cachedPostsByFilter[filter] ?: emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Post creation state
    var uploadSuccess by mutableStateOf(false)
        private set

    var uploadError by mutableStateOf("")
        private set

    init {
        fetchCurrentUserId()
        if (cachedPostsByFilter[filter].isNullOrEmpty()) {
            loadPosts()
        }
    }

    fun fetchCurrentUserId() {
        viewModelScope.launch {
            try {
                _currentUserId.value = userRequests.getUser()?.id
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error fetching user ID", e)
            }
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val posts = postRequests.getPosts(filter)
                _posts.value = posts
                // Cache posts for this filter
                cachedPostsByFilter[filter] = posts
                likeManager.registerPosts(posts)
            } catch (e: Exception) {
                _error.value = "Failed to load posts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            likeManager.likePost(postId)
        }
    }

    fun unlikePost(postId: String) {
        viewModelScope.launch {
            likeManager.unlikePost(postId)
        }
    }

    fun createPost(
        title: String,
        content: String,
        imageFile: File? = null,
        imageUri: Uri? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            try {
                uploadSuccess = false
                uploadError = ""

                val fileToUpload = when {
                    // Case 1: File is directly provided
                    imageFile != null -> imageFile

                    // Case 2: Uri needs to be converted to file
                    imageUri != null && context != null -> {
                        val tempFile = File.createTempFile("upload_image", ".jpg", context.cacheDir)
                        Log.d("PostViewModel", "Converting URI to file: $imageUri")

                        context.contentResolver.openInputStream(imageUri)?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: throw IOException("Failed to open input stream for $imageUri")

                        if (tempFile.length() == 0L) {
                            throw IOException("Failed to copy image data: file is empty")
                        }

                        tempFile
                    }

                    else -> throw IllegalArgumentException("Either file or both uri and context must be provided")
                }

                // Upload using the file
                val result = postRequests.createPost(fileToUpload, title, content)

                // Clean up temp file if we created one
                if (imageFile == null && fileToUpload.exists()) {
                    fileToUpload.delete()
                }

                if (result == null) {
                    uploadError = "Failed to create post: No response from server"
                } else if (result.id.toString().isBlank()) {
                    uploadError = "Failed to create post: Invalid post ID returned"
                } else {
                    uploadSuccess = true

                    // Invalidate all relevant caches
                    cachedPostsByFilter.remove("forYou")    // Main feed
                    cachedPostsByFilter.remove("profile")   // Current user's profile

                    // Remove any user_* caches that might exist for the current user
                    _currentUserId.value?.let { userId ->
                        cachedPostsByFilter.remove("user_$userId")
                    }

                    // If current filter is one of the affected ones, reload posts
                    if (filter == "forYou" || filter == "profile" ||
                        (_currentUserId.value != null && filter == "user_${_currentUserId.value}")
                    ) {
                        loadPosts()
                    }
                }

            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating post", e)
                uploadError = e.message ?: "Unknown error occurred"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Use the abstracted method in PostRequests
                val success = postRequests.deletePost(postId)

                if (success) {
                    // Remove post from local list immediately for better UX
                    _posts.value = _posts.value.filter { it.id != postId }

                    // Refresh posts list
                    loadPosts()
                } else {
                    _error.value = "Failed to delete post: permission denied"
                }
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting post", e)
                _error.value = "Failed to delete post: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isCurrentUserPostOwner(postUserId: String): Boolean {
        return _currentUserId.value == postUserId
    }

    class Factory(
        private val api: BreezyAPI,
        private val filter: String = "forYou",
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
                return PostViewModel(api, UserRequests(api), PostRequests(api), filter) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Simple factory for creating a PostViewModel for the creation flow
class PostViewModelFactory(private val api: BreezyAPI) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            return PostViewModel(api, UserRequests(api), PostRequests(api), "forYou") as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}