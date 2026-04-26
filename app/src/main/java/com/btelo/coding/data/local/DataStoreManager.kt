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
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_WS_TOKEN = "ws_token"
        private const val KEY_CLAUDE_SESSION_ID = "claude_session_id"
        
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

    /**
     * Get token synchronously (for immediate use)
     */
    fun getTokenSync(): String? {
        return try {
            encryptedPrefs.getString(KEY_TOKEN, null)
        } catch (e: Exception) {
            null
        } ?: regularPrefs.getString(KEY_TOKEN, null)
    }

    /**
     * Get token flow
     */
    fun getTokenFlow(): Flow<String?> {
        return kotlinx.coroutines.flow.flow {
            val token = try {
                encryptedPrefs.getString(KEY_TOKEN, null)
            } catch (e: Exception) {
                null
            } ?: regularPrefs.getString(KEY_TOKEN, null)
            emit(token)
        }
    }
    
    suspend fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun saveTokenFallback(token: String) {
        regularPrefs.edit().putString(KEY_TOKEN, token).apply()
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

    // ========== Session ID (server-assigned) ==========

    val sessionId: Flow<String?> = kotlinx.coroutines.flow.flow {
        emit(regularPrefs.getString(KEY_SESSION_ID, null))
    }

    fun getSessionIdSync(): String? {
        return regularPrefs.getString(KEY_SESSION_ID, null)
    }

    suspend fun saveSessionId(sessionId: String) {
        regularPrefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }

    suspend fun clearSessionId() {
        regularPrefs.edit().remove(KEY_SESSION_ID).apply()
    }

    // ========== Server Address (sync) ==========

    fun getServerAddressSync(): String? {
        return regularPrefs.getString(KEY_SERVER_ADDRESS, null)
    }

    // ========== WebSocket Token ==========

    fun getWsTokenSync(): String? {
        return regularPrefs.getString(KEY_WS_TOKEN, null)
    }

    suspend fun saveWsToken(token: String) {
        regularPrefs.edit().putString(KEY_WS_TOKEN, token).apply()
    }

    // ========== Claude Session ID ==========

    fun getClaudeSessionIdSync(): String? {
        return regularPrefs.getString(KEY_CLAUDE_SESSION_ID, null)
    }

    suspend fun saveClaudeSessionId(sessionId: String) {
        regularPrefs.edit().putString(KEY_CLAUDE_SESSION_ID, sessionId).apply()
    }

    suspend fun clearConnection() {
        regularPrefs.edit()
            .remove(KEY_SERVER_ADDRESS)
            .remove(KEY_SESSION_ID)
            .remove(KEY_WS_TOKEN)
            .remove(KEY_CLAUDE_SESSION_ID)
            .remove(KEY_DEVICE_ID)
            .apply()
    }
    
    // ========== Combined Auth Save/Clear ==========
    
    suspend fun saveAuth(token: String, userId: String, username: String, serverAddress: String) {
        // Save token to both encrypted and regular prefs (fallback)
        try {
            encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
        } catch (e: Exception) {
            // Encrypted prefs failed, save to regular prefs only
        }
        regularPrefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_SERVER_ADDRESS, serverAddress)
            .apply()
    }
    
    suspend fun clearAuth() {
        encryptedPrefs.edit().clear().apply()
        regularPrefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_SERVER_ADDRESS)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_SESSION_ID)
            .remove(KEY_WS_TOKEN)
            .apply()
    }
    
    /**
     * Check if token exists (for quick auth check)
     */
    fun hasToken(): Boolean {
        val encryptedToken = try {
            encryptedPrefs.getString(KEY_TOKEN, null)
        } catch (e: Exception) {
            null
        }
        return encryptedToken != null || regularPrefs.getString(KEY_TOKEN, null) != null
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
    
    // ========== Notification Settings ==========
    
    private val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private val KEY_NOTIFICATION_SOUND = "notification_sound"
    private val KEY_NOTIFICATION_VIBRATION = "notification_vibration"
    private val KEY_NOTIFICATION_LED = "notification_led"
    private val KEY_DND_ENABLED = "dnd_enabled"
    private val KEY_DND_START_HOUR = "dnd_start_hour"
    private val KEY_DND_START_MINUTE = "dnd_start_minute"
    private val KEY_DND_END_HOUR = "dnd_end_hour"
    private val KEY_DND_END_MINUTE = "dnd_end_minute"
    private val KEY_FCM_TOKEN = "fcm_token"
    
    // Notification enabled
    fun isNotificationEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }
    
    fun setNotificationEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }
    
    // Notification sound
    fun isNotificationSoundEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_NOTIFICATION_SOUND, true)
    }
    
    fun setNotificationSoundEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply()
    }
    
    // Notification vibration
    fun isNotificationVibrationEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_NOTIFICATION_VIBRATION, true)
    }
    
    fun setNotificationVibrationEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_NOTIFICATION_VIBRATION, enabled).apply()
    }
    
    // Notification LED
    fun isNotificationLedEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_NOTIFICATION_LED, true)
    }
    
    fun setNotificationLedEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_NOTIFICATION_LED, enabled).apply()
    }
    
    // DND enabled
    fun isDndEnabled(): Boolean {
        return regularPrefs.getBoolean(KEY_DND_ENABLED, false)
    }
    
    fun setDndEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_DND_ENABLED, enabled).apply()
    }
    
    // DND start time
    fun getDndStartHour(): Int {
        return regularPrefs.getInt(KEY_DND_START_HOUR, 22) // Default: 22:00
    }
    
    fun getDndStartMinute(): Int {
        return regularPrefs.getInt(KEY_DND_START_MINUTE, 0)
    }
    
    fun setDndStartTime(hour: Int, minute: Int) {
        regularPrefs.edit()
            .putInt(KEY_DND_START_HOUR, hour)
            .putInt(KEY_DND_START_MINUTE, minute)
            .apply()
    }
    
    // DND end time
    fun getDndEndHour(): Int {
        return regularPrefs.getInt(KEY_DND_END_HOUR, 7) // Default: 07:00
    }
    
    fun getDndEndMinute(): Int {
        return regularPrefs.getInt(KEY_DND_END_MINUTE, 0)
    }
    
    fun setDndEndTime(hour: Int, minute: Int) {
        regularPrefs.edit()
            .putInt(KEY_DND_END_HOUR, hour)
            .putInt(KEY_DND_END_MINUTE, minute)
            .apply()
    }
    
    // FCM Token
    suspend fun saveFcmToken(token: String) {
        regularPrefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }
    
    fun getFcmToken(): String? {
        return regularPrefs.getString(KEY_FCM_TOKEN, null)
    }
}
