package com.example.chatapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    private val EMAIL = stringPreferencesKey("email")
    private val PASSWORD = stringPreferencesKey("password")

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[EMAIL]
    }

    suspend fun saveUserCredentials(email: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[EMAIL] = email
            preferences[PASSWORD] = password
        }
    }

    suspend fun clearUserCredentials() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 