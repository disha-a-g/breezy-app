package com.whirlyfan.breezy.api

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val content: String,
    @SerialName("channel_id") val channelId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val username: String? = null // For display purposes
)

class MessageRequests(
    private val api: BreezyAPI
) {
    suspend fun deleteMessage(messageId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.client.from("messages")
                .delete {
                    filter {
                        eq("id", messageId)
                    }
                }
            true
        } catch (e: Exception) {
            false
        }
    }
}