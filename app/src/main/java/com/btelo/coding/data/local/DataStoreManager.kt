package com.btelo.coding.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "btelo_settings"
        private const val ENCRYPTED_PREFS_NAME = "btelo_encrypted_settings"
        
        // Non-sensitive keys (stored in regular preferences)
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_DEVICE_ID = "device_id"
        
        // Sensitive keys (stored in encrypted preferences)
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
    
    // Master key for encrypted preferences
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    // Encrypted preferences for sensitive data (tokens)
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    // Regular preferences for non-sensitive data
    private val regularPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // ========== Token (Encrypted) ==========
    
    val token: Flow<String?> = object : Flow<String?> {
        override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<String?>) {
            // Direct read for synchronous access
        }
    }
    
    /**
     * Get token synchronously (for immediate use)
     */
    fun getTokenSync(): String? {
        return encryptedPrefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Get token flow
     */
    fun getTokenFlow(): Flow<String?> {
        return kotlinx.coroutines.flow.flow {
            emit(encryptedPrefs.getString(KEY_TOKEN, null))
        }
    }
    
    suspend fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    suspend fun clearToken() {
        encryptedPrefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }
    
    // ========== User ID (Non-encrypted) ==========
    
    val userId: Flow<String?> = kotlinx.coroutines.flow.flow {
        emit(regularPrefs.getString(KEY_USER_ID, null))
    }
    
    suspend fun saveUserId(userId: String) {
        regularPrefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    // ========== Username (Non-encrypted) ==========
    
    val username: Flow<String?> = kotlinx.coroutines.flow.flow {
        emit(regularPrefs.getString(KEY_USERNAME, null))
    }
    
    suspend fun saveUsername(username: String) {
        regularPrefs.edit().putString(KEY_USERNAME, username).apply()
    }
    
    // ========== Server Address (Non-encrypted) ==========
    
    val serverAddress: Flow<String?> = kotlinx.coroutines.flow.flow {
        emit(regularPrefs.getString(KEY_SERVER_ADDRESS, null))
    }
    
    suspend fun saveServerAddress(serverAddress: String) {
        regularPrefs.edit().putString(KEY_SERVER_ADDRESS, serverAddress).apply()
    }
    
    // ========== Device ID (Non-encrypted) ==========
    
    val deviceId: Flow<String?> = kotlinx.coroutines.flow.flow {
        emit(regularPrefs.getString(KEY_DEVICE_ID, null))
    }
    
    suspend fun saveDeviceId(deviceId: String) {
        regularPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
    
    // ========== Combined Auth Save/Clear ==========
    
    suspend fun saveAuth(token: String, userId: String, username: String, serverAddress: String) {
        // Save token encrypted
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
        // Save other data in regular prefs
        regularPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_SERVER_ADDRESS, serverAddress)
            .apply()
    }
    
    suspend fun clearAuth() {
        encryptedPrefs.edit().clear().apply()
        regularPrefs.edit().clear().apply()
    }
    
    /**
     * Check if token exists (for quick auth check)
     */
    fun hasToken(): Boolean {
        return encryptedPrefs.getString(KEY_TOKEN, null) != null
    }
    
    // ========== Sync Related Keys ==========
    
    private val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    private val KEY_SYNC_ENABLED = "sync_enabled"
    private val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    
    // ========== Device ID Sync (Synchronous access) ==========
    
    /**
     * Get device ID synchronously (for sync operations)
     */
    fun getDeviceIdSync(): String? {
        return regularPrefs.getString(KEY_DEVICE_ID, null)
    }
    
    // ========== Last Sync Timestamp ==========
    
    /**
     * Get last sync timestamp
     */
    fun getLastSyncTimestamp(): Long {
        return regularPrefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }
    
    /**
     * Save last sync timestamp
     */
    fun saveLastSyncTimestamp(timestamp: Long) {
        regularPrefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }
    
    // ========== Sync Settings ==========
    
    /**
     * Check if sync is enabled
     */
    fun isSyncEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_SYNC_ENABLED, true)
    }
    
    /**
     * Set sync enabled
     */
    fun setSyncEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }
    
    /**
     * Check if auto sync is enabled
     */
    fun isAutoSyncEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }
    
    /**
     * Set auto sync enabled
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }
}
