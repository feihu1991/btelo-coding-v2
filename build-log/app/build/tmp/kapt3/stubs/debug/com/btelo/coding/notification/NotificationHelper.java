package com.btelo.coding.notification;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.btelo.coding.MainActivity;
import com.btelo.coding.R;
import com.btelo.coding.data.local.DataStoreManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.Calendar;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper class for creating and managing notifications
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0000\n\u0002\u0010\u000b\n\u0002\b\b\b\u0007\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0007\u001a\u00020\bJ\u000e\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000bJ\u0006\u0010\f\u001a\u00020\bJ,\u0010\r\u001a\u00020\u000e2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u000f\u001a\u00020\u000b2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\u0011H\u0002J\u0006\u0010\u0012\u001a\u00020\u0013J\b\u0010\u0014\u001a\u00020\u0013H\u0002J\u0006\u0010\u0015\u001a\u00020\u0013J2\u0010\u0016\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u000b2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u000b0\u0011J\u0016\u0010\u0019\u001a\u00020\b2\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u000bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/btelo/coding/notification/NotificationHelper;", "", "context", "Landroid/content/Context;", "dataStoreManager", "Lcom/btelo/coding/data/local/DataStoreManager;", "(Landroid/content/Context;Lcom/btelo/coding/data/local/DataStoreManager;)V", "cancelAllNotifications", "", "cancelChatNotification", "sessionId", "", "cancelSyncNotification", "createNotificationIntent", "Landroid/content/Intent;", "type", "data", "", "isAppInForeground", "", "isInDoNotDisturbPeriod", "shouldShowNotification", "showChatNotification", "title", "body", "showSyncNotification", "Companion", "app_debug"})
public final class NotificationHelper {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.btelo.coding.data.local.DataStoreManager dataStoreManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "NotificationHelper";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID_CHAT = "chat_messages";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID_SYNC = "sync_status";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID_GENERAL = "general";
    public static final int NOTIFICATION_ID_CHAT_BASE = 1000;
    public static final int NOTIFICATION_ID_SYNC = 2000;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_SESSION_ID = "session_id";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_NOTIFICATION_TYPE = "notification_type";
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.notification.NotificationHelper.Companion Companion = null;
    
    @javax.inject.Inject()
    public NotificationHelper(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager dataStoreManager) {
        super();
    }
    
    /**
     * Check if the app is currently in the foreground
     */
    public final boolean isAppInForeground() {
        return false;
    }
    
    /**
     * Check if notification should be shown (considers DND settings)
     */
    public final boolean shouldShowNotification() {
        return false;
    }
    
    /**
     * Check if current time is within Do Not Disturb period
     */
    private final boolean isInDoNotDisturbPeriod() {
        return false;
    }
    
    /**
     * Show a chat notification
     */
    public final void showChatNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String body, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    /**
     * Show a sync status notification
     */
    public final void showSyncNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String body) {
    }
    
    /**
     * Cancel sync notification
     */
    public final void cancelSyncNotification() {
    }
    
    /**
     * Cancel all chat notifications for a session
     */
    public final void cancelChatNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String sessionId) {
    }
    
    /**
     * Cancel all notifications
     */
    public final void cancelAllNotifications() {
    }
    
    /**
     * Create intent for notification click
     */
    private final android.content.Intent createNotificationIntent(java.lang.String sessionId, java.lang.String type, java.util.Map<java.lang.String, java.lang.String> data) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\nX\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/btelo/coding/notification/NotificationHelper$Companion;", "", "()V", "CHANNEL_ID_CHAT", "", "CHANNEL_ID_GENERAL", "CHANNEL_ID_SYNC", "EXTRA_NOTIFICATION_TYPE", "EXTRA_SESSION_ID", "NOTIFICATION_ID_CHAT_BASE", "", "NOTIFICATION_ID_SYNC", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}