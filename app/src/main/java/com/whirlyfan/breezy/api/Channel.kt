package com.whirlyfan.breezy.api

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.text.insert

@Serializable
data class Channel(
    val id: String,
    val name: String?,
    @SerialName("last_message_at") val lastMessageAt: String?,
    @SerialName("last_message_content") val lastMessageContent: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("user_id") val createdBy: String
)

@Serializable
data class ChannelWithMembers(
    val channel: Channel,
    val members: List<ChannelUser>
)

@Serializable
data class ChannelUser(
    val id: String,
    val username: String?
)

class ChannelRequests(
    private val api: BreezyAPI
) {
    private val userRequests = UserRequests(api)

    // Get the current user ID from cached user
    private suspend fun getCurrentUserId(): String {
        return userRequests.getUser()?.id ?: ""
    }

    // Get channels user is part of
    // TODO: Double fetching, need to fix this
    suspend fun getUserChannels(): List<ChannelWithMembers> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return@withContext emptyList()

        // Get channel IDs the user is a member of
        val channelIds = api.client.from("channel_members")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<Map<String, String>>()
            .mapNotNull { it["channel_id"] }
            .filter { it.isNotEmpty() }

        if (channelIds.isEmpty()) {
            return@withContext emptyList()
        }

        // Get channel details
        val channels = api.client.from("channels")
            .select {
                filter {
                    isIn("id", channelIds)
                }
            }
            .decodeList<Channel>()

        // Get all members for these channels
        val allChannelMembers = api.client.from("channel_members")
            .select {
                filter {
                    isIn("channel_id", channelIds)
                }
            }
            .decodeList<Map<String, String>>()

        // Extract all user IDs from channel members
        val userIds = allChannelMembers.mapNotNull { it["user_id"] }.distinct()

        // Fetch user details for all member IDs
        val users = api.client.from("users") // Adjust table name if different
            .select(columns = Columns.list("username", "id")) {
                filter {
                    isIn("id", userIds)
                }
            }
            .decodeList<Map<String, String>>()

        // Create a map of user ID to user details
        val userMap = users.associate {
            it["id"]!! to ChannelUser(
                id = it["id"]!!,
                username = it["username"] ?: "Unknown"
            )
        }

        // Group members by channel
        val membersByChannel = allChannelMembers.groupBy { it["channel_id"] }

        // Create ChannelWithMembers objects
        channels.map { channel ->
            val channelMemberIds =
                membersByChannel[channel.id]?.mapNotNull { it["user_id"] } ?: emptyList()
            val channelMembers = channelMemberIds.mapNotNull { userId -> userMap[userId] }
            ChannelWithMembers(channel, channelMembers)
        }
    }

    // Delete a channel
    suspend fun deleteChannel(channelId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.client.from("channels")
                .delete {
                    filter {
                        eq("id", channelId)
                    }
                }
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun createChannel(name: String?, initialMembers: List<String> = emptyList()): Channel? =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                if (userId.isEmpty()) return@withContext null

                // Create new channel
                val response = api.client.from("channels")
                    .insert(
                        value = mapOf(
                            "name" to name,
                            "user_id" to userId
                        ),
                    ) {
                        // Ensure we get the created channel back to add members
                        select()
                    }
                    // Use decodeList instead of decodeSingle as a precaution when selecting because Single can return an error if no rows are found vs List will just return an empty list
                    .decodeList<Channel>()

                if (response.isEmpty()) {
                    Log.e("ChannelRequests", "Channel creation response was empty")
                    return@withContext null
                }

                val createdChannel = response.first()

                // Add additional members if provided (excluding current user who is already added by trigger)
                initialMembers.forEach { memberId ->
                    if (memberId != userId) {
                        try {
                            api.client.from("channel_members")
                                .insert(
                                    value = mapOf(
                                        "channel_id" to createdChannel.id,
                                        "user_id" to memberId
                                    ),
                                )
                        } catch (e: Exception) {
                            Log.e("ChannelRequests", "Error adding member $memberId", e)
                        }
                    }
                }

                return@withContext createdChannel
            } catch (e: Exception) {
                Log.e("ChannelRequests", "Error creating channel", e)
                return@withContext null
            }
        }

    suspend fun createDirectMessageChannel(targetUserId: String): Channel =
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = getCurrentUserId()
                if (currentUserId.isEmpty()) throw Exception("Not authenticated")

                // Check if a DM channel between these users already exists
                val existingChannels = getUserChannels()
                val existingDM = existingChannels.find { channelWithMembers ->
                    channelWithMembers.channel.name == null &&
                            channelWithMembers.members.size == 2 &&
                            channelWithMembers.members.any { it.id == targetUserId } &&
                            channelWithMembers.members.any { it.id == currentUserId }
                }

                // Return existing channel if found
                if (existingDM != null) {
                    return@withContext existingDM.channel
                }

                // Otherwise create a new DM channel (don't need to add current user as member again)
                val channel = createChannel(
                    name = null,
                    initialMembers = listOf(targetUserId) // Only add target user, current user is added by trigger
                ) ?: throw Exception("Failed to create channel")

                return@withContext channel
            } catch (e: Exception) {
                Log.e("ChannelRequests", "Error creating DM channel", e)
                throw Exception("Failed to create direct message channel: ${e.message}")
            }
        }
}