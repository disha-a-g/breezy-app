package com.whirlyfan.breezy.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whirlyfan.breezy.api.BreezyAPI
import com.whirlyfan.breezy.api.Message
import com.whirlyfan.breezy.api.MessageRequests
import com.whirlyfan.breezy.api.UserRequests
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.collections.plusAssign
import kotlin.text.get

class MessageViewModel(
    private val api: BreezyAPI,
    private val channelId: String,
    displayName: String
) : ViewModel() {
    private val messageRequests = MessageRequests(api)
    private val userRequests = UserRequests(api)
    private var messageChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var isSubscribed = false

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _channelName = MutableStateFlow(displayName)
    val channelName: StateFlow<String> = _channelName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _currentUserId.value = userRequests.getUser()?.id ?: ""
                loadMessages()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadMessages() {
        try {
            val messages = api.client.from("messages")
                .select {
                    filter {
                        eq("channel_id", channelId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Message>()

            // Fetch usernames for messages
            val userIds = messages.map { it.userId }.distinct()
            val users = api.client.from("users")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<Map<String, String?>>()

            val userMap = users.associate { it["id"]!! to (it["username"] ?: "Unknown") }

            _messages.value = messages.map { message ->
                message.copy(username = userMap[message.userId])
            }
        } catch (e: Exception) {
            _error.value = "Failed to load messages: ${e.message}"
        }
    }

    fun subscribeToMessages() {
        viewModelScope.launch {
            try {
                if (isSubscribed) return@launch

                val channel = api.client.channel(channelId)
                messageChannel = channel

                val changeFlow =
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "messages"
                        filter("channel_id", FilterOperator.EQ, channelId)
                    }

                changeFlow.onEach {
                    when (it) {
                        is PostgresAction.Insert -> {
                            val newMessage = it.toMessageOrNull()
                            if (newMessage != null) {
                                // Fetch username for the new message
                                val userName = try {
                                    api.client.from("users")
                                        .select { filter { eq("id", newMessage.userId) } }
                                        .decodeSingle<Map<String, String?>>()["username"]
                                        ?: "Unknown"
                                } catch (e: Exception) {
                                    "Unknown"
                                }
                                _messages.value += newMessage.copy(username = userName)
                            }
                        }

                        is PostgresAction.Delete -> {
                            Log.d("MessageViewModel", "Delete action received")
                            val record = it.oldRecord
                            Log.d("MessageViewModel", "Delete record: $record")
                            val deletedId = record["id"]?.toString()?.trim('"')
                            Log.d("MessageViewModel", "Deleted message ID: $deletedId")
                            if (deletedId != null) {
                                val initialCount = _messages.value.size
                                _messages.value = _messages.value.filter { it.id != deletedId }
                                Log.d(
                                    "MessageViewModel",
                                    "Messages before: $initialCount, after: ${_messages.value.size}"
                                )
                            }
                        }

                        is PostgresAction.Select -> TODO()
                        is PostgresAction.Update -> TODO()
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                isSubscribed = true
            } catch (e: Exception) {
                _error.value = "Failed to subscribe to messages: ${e.message}"
            }
        }
    }

    fun unsubscribeFromMessages() {
        viewModelScope.launch {
            try {
                Log.d("MessageViewModel", "Unsubscribing from message channel")
                messageChannel?.unsubscribe()
                messageChannel = null
                isSubscribed = false
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error unsubscribing: ${e.message}")
            }
        }
    }

    // onCleared is for safety and shouldn't be triggered normally
    override fun onCleared() {
        super.onCleared()
        unsubscribeFromMessages()
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _currentUserId.value.isEmpty()) return

        viewModelScope.launch {
            try {
                api.client.from("messages")
                    .insert(
                        value = buildJsonObject {
                            put("content", content)
                            put("channel_id", channelId)
                            put("user_id", _currentUserId.value)
                        }
                    )
            } catch (e: Exception) {
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    fun unsendMessage(messageId: String) {
        viewModelScope.launch {
//            _isLoading.value = true
            _error.value = null
            try {
                val success = messageRequests.deleteMessage(messageId)
                if (!success) {
                    _error.value = "Failed to unsend message"
                } else {
                    // Manually update the UI immediately after successful deletion
                    // THIS WILL NOT UPDATE UI ON OTHER DEVICES
                    _messages.value = _messages.value.filter { it.id != messageId }
                }
            } catch (e: Exception) {
                _error.value = "Error unsending message: ${e.message}"
            } finally {
//                _isLoading.value = false
            }
        }
    }

    private fun PostgresAction.Insert.toMessageOrNull(): Message? {
        return try {
            val data = this.record
            Message(
                id = data["id"]?.toString()?.trim('"') ?: "",
                content = data["content"]?.toString()?.trim('"') ?: "",
                channelId = data["channel_id"]?.toString()?.trim('"') ?: "",
                userId = data["user_id"]?.toString()?.trim('"') ?: "",
                createdAt = data["created_at"]?.toString()?.trim('"') ?: "",
                updatedAt = data["updated_at"]?.toString()?.trim('"') ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    class Factory(
        private val api: BreezyAPI,
        private val channelId: String,
        private val displayName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
                return MessageViewModel(api, channelId, displayName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}