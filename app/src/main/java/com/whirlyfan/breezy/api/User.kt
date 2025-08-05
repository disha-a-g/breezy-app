package com.whirlyfan.breezy.api

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.text.get

@Serializable
data class User(
    val id: String,
    val username: String?,
    val bio: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

class UserRequests(
    private val api: BreezyAPI,
) {
    private val query = api.client.from("users")
    private var cachedUserId: String? = null
    private var cachedUser: User? = null

    private fun getUserId(): String {
        if (cachedUserId == null) {
            cachedUserId =
                api.client.auth
                    .currentUserOrNull()
                    ?.id
        }
        return cachedUserId ?: ""
    }

    fun clearCache() {
        cachedUserId = null
        cachedUser = null
    }

    // Get user details with caching
    suspend fun getUser(): User? {
        return withContext(Dispatchers.IO) {
            val userId = getUserId()
            if (userId.isEmpty()) return@withContext null

            // TODO: Fix cachedUser. Currently breaks set username as it won't fetch the updated username if user is already present.
//            // Return cached user if available
//            cachedUser?.let { return@withContext it }

            try {
                // Fetch user from database
                val user =
                    query
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }.decodeSingle<User>()

                // Cache the user
                cachedUser = user
                user
            } catch (e: Exception) {
                null
            }
        }
    }


    // Reset cache when auth state changes
    fun setAuthenticatedUser(newUserId: String?) {
        cachedUserId = newUserId
        cachedUser = null
    }

    suspend fun updateUser(
        username: String? = null,
        bio: String? = null,
        onSuccess: () -> Unit = {
            // This should run checkUserProfile() in the MainViewModel
        },
    ) {
        withContext(Dispatchers.IO) {
            try {
                val userId = getUserId()
                if (userId.isEmpty()) throw Exception("User not authenticated")

                val user =
                    query
                        .update(
                            {
                                if (!username.isNullOrBlank()) {
                                    set("username", username)
                                }
                                if (!bio.isNullOrBlank()) {
                                    set("bio", bio)
                                }
                            },
                        ) {
                            select()
                            filter {
                                eq("id", userId)
                            }
                        }.decodeSingle<User>()

                cachedUser = user
                onSuccess()
            } catch (e: Exception) {
                Log.e("UserRequests", "Error updating user: ${e.localizedMessage}")
                throw e
            }
        }
    }

    suspend fun searchUsers(query: String): List<User> =
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = getUserId()
                api.client.from("users")
                    .select {
                        filter {
                            ilike("username", "%$query%")
                            if (currentUserId.isNotEmpty()) {
                                neq("id", currentUserId)
                            }
                        }
                        order("username", order = Order.ASCENDING)
                    }
                    .decodeList<User>()
            } catch (e: Exception) {
                emptyList()
            }
        }

    // Get user by ID
    suspend fun getUserById(userId: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                query
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }.decodeSingle<User>()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getFollowersCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                api.client
                    .from("followers")
                    .select {
                        filter {
                            eq("following_id", userId)
                        }
                        count(Count.EXACT)
                    }
                    .countOrNull() ?: 0
            } catch (e: Exception) {
                Log.e("UserRequests", "Error getting followers count: ${e.message}")
                0
            }.toInt()
        }
    }

    suspend fun getFollowingCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                api.client
                    .from("followers")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                        count(Count.EXACT)
                    }
                    .countOrNull() ?: 0
            } catch (e: Exception) {
                Log.e("UserRequests", "Error getting following count: ${e.message}")
                0
            }.toInt()
        }
    }

    suspend fun isFollowing(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = getUserId()
                if (currentUserId.isEmpty()) return@withContext false

                val count = api.client.from("followers")
                    .select {
                        filter {
                            eq("user_id", currentUserId)
                            eq("following_id", userId)
                        }
                        count(Count.EXACT)
                    }
                    .countOrNull() ?: 0

                count > 0
            } catch (e: Exception) {
                Log.e("UserRequests", "Error checking follow status: ${e.message}")
                false
            }
        }
    }

    suspend fun follow(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUserId = getUserId()
            if (currentUserId.isEmpty()) throw Exception("Not authenticated")

            try {
                // Check first to avoid unnecessary operations
                if (isFollowing(userId)) {
                    return@withContext true // Already following
                }

                // Use upsert to handle potential race conditions
                api.client.from("followers")
                    .upsert(
                        mapOf(
                            "user_id" to currentUserId,
                            "following_id" to userId
                        )
                    )

                true // Follow succeeded
            } catch (e: Exception) {
                Log.e("UserRequests", "Error following user: ${e.message}")
                false
            }
        }
    }

    suspend fun unfollow(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUserId = getUserId()
            if (currentUserId.isEmpty()) throw Exception("Not authenticated")

            try {
                // Check first to avoid unnecessary operations
                if (!isFollowing(userId)) {
                    return@withContext true // Already not following
                }

                api.client.from("followers")
                    .delete {
                        filter {
                            eq("user_id", currentUserId)
                            eq("following_id", userId)
                        }
                    }

                true // Unfollow succeeded
            } catch (e: Exception) {
                Log.e("UserRequests", "Error unfollowing user: ${e.message}")
                false
            }
        }
    }

    suspend fun getFollowers(userId: String): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetching across a many-to-many relationship in Supabase Kotlin requires two steps:
                // There might be a way to do this in a single query
                // First get the IDs of users following this user
                val followerIds = api.client
                    .from("followers")
                    .select(
                        columns = Columns.list("following_id"),
                    ) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<Map<String, String>>().mapNotNull { it["following_id"] }
                if (followerIds.isEmpty()) {
                    return@withContext emptyList<User>()
                }

                // Then fetch the actual user objects for those IDs
                api.client
                    .from("users")
                    .select {
                        filter {
                            isIn("id", followerIds)
                        }
                    }
                    .decodeList<User>()
            } catch (e: Exception) {
                Log.e("UserRequests", "Error getting followers: ${e.message}")
                emptyList<User>()
            }
        }
    }

    suspend fun getFollowing(userId: String): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                // There might be a way to do this in a single query
                // Get the IDs of users that this user is following
                val followingIds = api.client
                    .from("followers")
                    .select(
                        columns = Columns.list("user_id"),
                    ) {
                        filter {
                            eq("following_id", userId)
                        }
                    }
                    .decodeList<Map<String, String>>().mapNotNull { it["user_id"] }

                if (followingIds.isEmpty()) {
                    return@withContext emptyList<User>()
                }

                // Fetch the actual user objects for those IDs
                api.client
                    .from("users")
                    .select {
                        filter {
                            isIn("id", followingIds)
                        }
                    }
                    .decodeList<User>()
            } catch (e: Exception) {
                Log.e("UserRequests", "Error getting following: ${e.message}")
                emptyList<User>()
            }
        }
    }
}