package com.example.sosapp.data.local.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.sosapp.data.model.UserData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create a DataStore instance scoped to Context.
// The DataStore is named "user_cache" and is used for persisting user-related data.
private val Context.userDataStore by preferencesDataStore("user_cache")


/**
 * Implementation of [UserCache] that uses DataStore for local caching of user data.
 * This class provides methods to save, retrieve, and clear user information
 * using a JSON representation stored in the DataStore.
 *
 * Annotated with @Singleton to ensure only one instance is used throughout the app,
 * and @Inject to allow Hilt to provide it wherever needed.
 *
 * @param context The application context used to access the DataStore.
 * @param gson The Gson instance for serializing and deserializing UserData objects.
 */
@Singleton
class UserCacheImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : UserCache {

    companion object {
        // Key used to store user data in the DataStore.
        val USER_DATA_KEY = stringPreferencesKey("user_data")
    }

    /**
     * Saves the provided [UserData] object to the local cache.
     * The user data is serialized to JSON format using Gson and stored in DataStore.
     *
     * @param user The [UserData] object containing user information to be saved.
     */
    override suspend fun saveUser(user: UserData) {
        context.userDataStore.edit { preferences ->
            preferences[USER_DATA_KEY] = gson.toJson(user)
        }
    }

    /**
     * Retrieves the currently saved [UserData] from the local cache.
     * If no user data is found, it returns `null`.
     *
     * @return The stored [UserData] if available, or `null` if no user is cached.
     */
    override suspend fun getUser(): UserData? {
        return try {
            val userJson = context.userDataStore.data
                .map { preferences -> preferences[USER_DATA_KEY] }
                .first()

            userJson?.let { gson.fromJson(it, UserData::class.java) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears any stored user data from the local cache.
     * This is typically used during logout or account switch.
     */
    override suspend fun clearUser() {
        context.userDataStore.edit { preferences ->
            preferences.remove(USER_DATA_KEY)
        }
    }
}