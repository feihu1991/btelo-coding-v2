package com.btelo.coding.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.btelo.coding.notification.NotificationChannelManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Receiver for device boot to reinitialize FCM
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u000f2\u00020\u0001:\u0001\u000fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u0010"}, d2 = {"Lcom/btelo/coding/push/BootReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "notificationChannelManager", "Lcom/btelo/coding/notification/NotificationChannelManager;", "getNotificationChannelManager", "()Lcom/btelo/coding/notification/NotificationChannelManager;", "setNotificationChannelManager", "(Lcom/btelo/coding/notification/NotificationChannelManager;)V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "Companion", "app_debug"})
public final class BootReceiver extends android.content.BroadcastReceiver {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BootReceiver";
    @javax.inject.Inject()
    public com.btelo.coding.notification.NotificationChannelManager notificationChannelManager;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.push.BootReceiver.Companion Companion = null;
    
    public BootReceiver() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.notification.NotificationChannelManager getNotificationChannelManager() {
        return null;
    }
    
    public final void setNotificationChannelManager(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.notification.NotificationChannelManager p0) {
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/btelo/coding/push/BootReceiver$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}