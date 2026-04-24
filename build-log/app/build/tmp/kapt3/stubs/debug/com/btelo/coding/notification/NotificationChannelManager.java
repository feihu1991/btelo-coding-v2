package com.btelo.coding.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages notification channels for Android O and above
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u000b\u001a\u00020\fH\u0002J\b\u0010\r\u001a\u00020\fH\u0002J\u0006\u0010\u000e\u001a\u00020\fJ\b\u0010\u000f\u001a\u00020\fH\u0002J\u0006\u0010\u0010\u001a\u00020\fJ\u001e\u0010\u0011\u001a\u00020\f2\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0015\u001a\u00020\u0013R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\b\u00a8\u0006\u0017"}, d2 = {"Lcom/btelo/coding/notification/NotificationChannelManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "notificationManager", "Landroid/app/NotificationManager;", "getNotificationManager", "()Landroid/app/NotificationManager;", "notificationManager$delegate", "Lkotlin/Lazy;", "createChatChannel", "", "createGeneralChannel", "createNotificationChannels", "createSyncChannel", "deleteAllChannels", "updateChatChannelSettings", "soundEnabled", "", "vibrationEnabled", "ledEnabled", "Companion", "app_debug"})
public final class NotificationChannelManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "NotificationChannelMgr";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHAT_CHANNEL_DESC = "Chat message notifications";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SYNC_CHANNEL_DESC = "Sync status notifications";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String GENERAL_CHANNEL_DESC = "General app notifications";
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy notificationManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.notification.NotificationChannelManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public NotificationChannelManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final android.app.NotificationManager getNotificationManager() {
        return null;
    }
    
    /**
     * Create all notification channels required by the app
     */
    public final void createNotificationChannels() {
    }
    
    /**
     * Create chat messages notification channel
     */
    private final void createChatChannel() {
    }
    
    /**
     * Create sync status notification channel
     */
    private final void createSyncChannel() {
    }
    
    /**
     * Create general notifications channel
     */
    private final void createGeneralChannel() {
    }
    
    /**
     * Update chat channel settings
     */
    public final void updateChatChannelSettings(boolean soundEnabled, boolean vibrationEnabled, boolean ledEnabled) {
    }
    
    /**
     * Delete all notification channels
     */
    public final void deleteAllChannels() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/btelo/coding/notification/NotificationChannelManager$Companion;", "", "()V", "CHAT_CHANNEL_DESC", "", "GENERAL_CHANNEL_DESC", "SYNC_CHANNEL_DESC", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}