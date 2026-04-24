package com.btelo.coding.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b \b\u0007\u0018\u0000 \\2\u00020\u0001:\u0001\\B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010-\u001a\u00020.H\u0086@\u00a2\u0006\u0002\u0010/J\u000e\u00100\u001a\u00020.H\u0086@\u00a2\u0006\u0002\u0010/J\b\u00101\u001a\u0004\u0018\u00010\u0006J\u0006\u00102\u001a\u000203J\u0006\u00104\u001a\u000203J\u0006\u00105\u001a\u000203J\u0006\u00106\u001a\u000203J\b\u00107\u001a\u0004\u0018\u00010\u0006J\u0006\u00108\u001a\u000209J\u000e\u0010:\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014J\b\u0010;\u001a\u0004\u0018\u00010\u0006J\u0006\u0010<\u001a\u00020=J\u0006\u0010>\u001a\u00020=J\u0006\u0010?\u001a\u00020=J\u0006\u0010@\u001a\u00020=J\u0006\u0010A\u001a\u00020=J\u0006\u0010B\u001a\u00020=J\u0006\u0010C\u001a\u00020=J\u0006\u0010D\u001a\u00020=J.\u0010E\u001a\u00020.2\u0006\u0010\'\u001a\u00020\u00062\u0006\u0010)\u001a\u00020\u00062\u0006\u0010+\u001a\u00020\u00062\u0006\u0010%\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010FJ\u0016\u0010G\u001a\u00020.2\u0006\u0010\u0013\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u0016\u0010I\u001a\u00020.2\u0006\u0010\'\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u000e\u0010J\u001a\u00020.2\u0006\u0010K\u001a\u000209J\u0016\u0010L\u001a\u00020.2\u0006\u0010%\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u0016\u0010M\u001a\u00020.2\u0006\u0010\'\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u0016\u0010N\u001a\u00020.2\u0006\u0010)\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u0016\u0010O\u001a\u00020.2\u0006\u0010+\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010HJ\u000e\u0010P\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u000e\u0010R\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u0016\u0010S\u001a\u00020.2\u0006\u0010T\u001a\u0002032\u0006\u0010U\u001a\u000203J\u0016\u0010V\u001a\u00020.2\u0006\u0010T\u001a\u0002032\u0006\u0010U\u001a\u000203J\u000e\u0010W\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u000e\u0010X\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u000e\u0010Y\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u000e\u0010Z\u001a\u00020.2\u0006\u0010Q\u001a\u00020=J\u000e\u0010[\u001a\u00020.2\u0006\u0010Q\u001a\u00020=R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0013\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u001b\u0010\u0017\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001b\u0010\u001c\u001a\u0004\b\u0019\u0010\u001aR\u001b\u0010\u001d\u001a\u00020\u001e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b!\u0010\u001c\u001a\u0004\b\u001f\u0010 R\u001b\u0010\"\u001a\u00020\u00188BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b$\u0010\u001c\u001a\u0004\b#\u0010\u001aR\u0019\u0010%\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0016R\u0019\u0010\'\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u0016R\u0019\u0010)\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u0016R\u0019\u0010+\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010\u0016\u00a8\u0006]"}, d2 = {"Lcom/btelo/coding/data/local/DataStoreManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "KEY_AUTO_SYNC_ENABLED", "", "KEY_DND_ENABLED", "KEY_DND_END_HOUR", "KEY_DND_END_MINUTE", "KEY_DND_START_HOUR", "KEY_DND_START_MINUTE", "KEY_FCM_TOKEN", "KEY_LAST_SYNC_TIMESTAMP", "KEY_NOTIFICATION_ENABLED", "KEY_NOTIFICATION_LED", "KEY_NOTIFICATION_SOUND", "KEY_NOTIFICATION_VIBRATION", "KEY_SYNC_ENABLED", "deviceId", "Lkotlinx/coroutines/flow/Flow;", "getDeviceId", "()Lkotlinx/coroutines/flow/Flow;", "encryptedPrefs", "Landroid/content/SharedPreferences;", "getEncryptedPrefs", "()Landroid/content/SharedPreferences;", "encryptedPrefs$delegate", "Lkotlin/Lazy;", "masterKey", "Landroidx/security/crypto/MasterKey;", "getMasterKey", "()Landroidx/security/crypto/MasterKey;", "masterKey$delegate", "regularPrefs", "getRegularPrefs", "regularPrefs$delegate", "serverAddress", "getServerAddress", "token", "getToken", "userId", "getUserId", "username", "getUsername", "clearAuth", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearToken", "getDeviceIdSync", "getDndEndHour", "", "getDndEndMinute", "getDndStartHour", "getDndStartMinute", "getFcmToken", "getLastSyncTimestamp", "", "getTokenFlow", "getTokenSync", "hasToken", "", "isAutoSyncEnabled", "isDndEnabled", "isNotificationEnabled", "isNotificationLedEnabled", "isNotificationSoundEnabled", "isNotificationVibrationEnabled", "isSyncEnabled", "saveAuth", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveDeviceId", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveFcmToken", "saveLastSyncTimestamp", "timestamp", "saveServerAddress", "saveToken", "saveUserId", "saveUsername", "setAutoSyncEnabled", "enabled", "setDndEnabled", "setDndEndTime", "hour", "minute", "setDndStartTime", "setNotificationEnabled", "setNotificationLedEnabled", "setNotificationSoundEnabled", "setNotificationVibrationEnabled", "setSyncEnabled", "Companion", "app_debug"})
public final class DataStoreManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "btelo_settings";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ENCRYPTED_PREFS_NAME = "btelo_encrypted_settings";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SERVER_ADDRESS = "server_address";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USERNAME = "username";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_USER_ID = "user_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_DEVICE_ID = "device_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TOKEN = "auth_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_REFRESH_TOKEN = "refresh_token";
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy masterKey$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy encryptedPrefs$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy regularPrefs$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.String> token = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.String> userId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.String> username = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.String> serverAddress = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.lang.String> deviceId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_SYNC_ENABLED = "sync_enabled";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_NOTIFICATION_SOUND = "notification_sound";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_NOTIFICATION_VIBRATION = "notification_vibration";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_NOTIFICATION_LED = "notification_led";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_DND_ENABLED = "dnd_enabled";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_DND_START_HOUR = "dnd_start_hour";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_DND_START_MINUTE = "dnd_start_minute";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_DND_END_HOUR = "dnd_end_hour";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_DND_END_MINUTE = "dnd_end_minute";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String KEY_FCM_TOKEN = "fcm_token";
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.data.local.DataStoreManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public DataStoreManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final androidx.security.crypto.MasterKey getMasterKey() {
        return null;
    }
    
    private final android.content.SharedPreferences getEncryptedPrefs() {
        return null;
    }
    
    private final android.content.SharedPreferences getRegularPrefs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getToken() {
        return null;
    }
    
    /**
     * Get token synchronously (for immediate use)
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getTokenSync() {
        return null;
    }
    
    /**
     * Get token flow
     */
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getTokenFlow() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearToken(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getUserId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveUserId(@org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getUsername() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveUsername(@org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getServerAddress() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveServerAddress(@org.jetbrains.annotations.NotNull()
    java.lang.String serverAddress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.lang.String> getDeviceId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveDeviceId(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveAuth(@org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    java.lang.String userId, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String serverAddress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object clearAuth(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Check if token exists (for quick auth check)
     */
    public final boolean hasToken() {
        return false;
    }
    
    /**
     * Get device ID synchronously (for sync operations)
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getDeviceIdSync() {
        return null;
    }
    
    /**
     * Get last sync timestamp
     */
    public final long getLastSyncTimestamp() {
        return 0L;
    }
    
    /**
     * Save last sync timestamp
     */
    public final void saveLastSyncTimestamp(long timestamp) {
    }
    
    /**
     * Check if sync is enabled
     */
    public final boolean isSyncEnabled() {
        return false;
    }
    
    /**
     * Set sync enabled
     */
    public final void setSyncEnabled(boolean enabled) {
    }
    
    /**
     * Check if auto sync is enabled
     */
    public final boolean isAutoSyncEnabled() {
        return false;
    }
    
    /**
     * Set auto sync enabled
     */
    public final void setAutoSyncEnabled(boolean enabled) {
    }
    
    public final boolean isNotificationEnabled() {
        return false;
    }
    
    public final void setNotificationEnabled(boolean enabled) {
    }
    
    public final boolean isNotificationSoundEnabled() {
        return false;
    }
    
    public final void setNotificationSoundEnabled(boolean enabled) {
    }
    
    public final boolean isNotificationVibrationEnabled() {
        return false;
    }
    
    public final void setNotificationVibrationEnabled(boolean enabled) {
    }
    
    public final boolean isNotificationLedEnabled() {
        return false;
    }
    
    public final void setNotificationLedEnabled(boolean enabled) {
    }
    
    public final boolean isDndEnabled() {
        return false;
    }
    
    public final void setDndEnabled(boolean enabled) {
    }
    
    public final int getDndStartHour() {
        return 0;
    }
    
    public final int getDndStartMinute() {
        return 0;
    }
    
    public final void setDndStartTime(int hour, int minute) {
    }
    
    public final int getDndEndHour() {
        return 0;
    }
    
    public final int getDndEndMinute() {
        return 0;
    }
    
    public final void setDndEndTime(int hour, int minute) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveFcmToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getFcmToken() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lcom/btelo/coding/data/local/DataStoreManager$Companion;", "", "()V", "ENCRYPTED_PREFS_NAME", "", "KEY_DEVICE_ID", "KEY_REFRESH_TOKEN", "KEY_SERVER_ADDRESS", "KEY_TOKEN", "KEY_USERNAME", "KEY_USER_ID", "PREFS_NAME", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}