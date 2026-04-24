package com.btelo.coding.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "btelo_settings")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_SERVER_ADDRESS = stringPreferencesKey("server_address")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[KEY_USERNAME] }
    val serverAddress: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER_ADDRESS] }

    suspend fun saveAuth(token: String, userId: String, username: String, serverAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USERNAME] = username
            preferences[KEY_SERVER_ADDRESS] = serverAddress
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { it.clear() }
    }
}
