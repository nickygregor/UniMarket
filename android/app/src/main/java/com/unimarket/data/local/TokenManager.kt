package com.unimarket.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unimarket.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "unimarket_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USER  = stringPreferencesKey("current_user")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val user : Flow<User?>   = context.dataStore.data.map { prefs ->
        prefs[KEY_USER]?.let { Json.decodeFromString<User>(it) }
    }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: false }

    fun profileImage(userId: Int): Flow<String?> =
        context.dataStore.data.map { it[stringPreferencesKey("profile_image_$userId")] }

    suspend fun saveSession(token: String, user: User) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_USER]  = Json.encodeToString(user)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USER)
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_DARK_MODE] = enabled
        }
    }

    suspend fun saveProfileImage(userId: Int, imageData: String) {
        context.dataStore.edit {
            it[stringPreferencesKey("profile_image_$userId")] = imageData
        }
    }

    suspend fun clearProfileImage(userId: Int) {
        context.dataStore.edit {
            it.remove(stringPreferencesKey("profile_image_$userId"))
        }
    }

    /** Returns "Bearer <token>" header value */
    suspend fun bearerToken(): String? =
        token.first()?.let { "Bearer $it" }
}
