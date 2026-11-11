package com.hhpc.smarttask.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[JWT_TOKEN_KEY]
    }

    val userNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY]
    }

    val userEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_EMAIL_KEY]
    }

    suspend fun saveAuth(token: String, email: String?, fullName: String?, avatarUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[JWT_TOKEN_KEY] = token
            if (email != null) prefs[USER_EMAIL_KEY] = email
            if (fullName != null) prefs[USER_NAME_KEY] = fullName
            if (avatarUrl != null) prefs[USER_AVATAR_KEY] = avatarUrl
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
