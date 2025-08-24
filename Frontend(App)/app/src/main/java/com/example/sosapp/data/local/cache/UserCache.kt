package com.example.sosapp.data.local.cache

import com.example.sosapp.data.model.UserData

/**
 * Interface for managing user data in a local cache.
 * This interface defines methods to save, retrieve, and clear user information
 * from the local storage, typically used for caching user sessions.
 */
interface UserCache {
    /**
     * Saves the given [UserData] object to the local cache.
     *
     * @param user The [UserData] object containing user information
     *             that needs to be stored locally.
     */
    suspend fun saveUser(user: UserData)

    /**
     * Retrieves the currently saved [UserData] from the local cache.
     *
     * @return The stored [UserData] if available, or `null` if
     *         no user is cached.
     */
    suspend fun getUser(): UserData?

    /**
     * Clears any stored user data from the local cache.
     * This is typically used during logout or account switch.
     */
    suspend fun clearUser()
}