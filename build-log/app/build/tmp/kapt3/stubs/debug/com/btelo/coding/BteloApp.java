package com.btelo.coding;

import android.app.Application;
import android.util.Log;
import com.btelo.coding.notification.NotificationChannelManager;
import com.btelo.coding.push.FcmTokenManager;
import dagger.hilt.android.HiltAndroidApp;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;

@dagger.hilt.android.HiltAndroidApp()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0011\u001a\u00020\u0012H\u0016R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0005\u001a\u00020\u00068\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR\u001e\u0010\u000b\u001a\u00020\f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0014"}, d2 = {"Lcom/btelo/coding/BteloApp;", "Landroid/app/Application;", "()V", "applicationScope", "Lkotlinx/coroutines/CoroutineScope;", "fcmTokenManager", "Lcom/btelo/coding/push/FcmTokenManager;", "getFcmTokenManager", "()Lcom/btelo/coding/push/FcmTokenManager;", "setFcmTokenManager", "(Lcom/btelo/coding/push/FcmTokenManager;)V", "notificationChannelManager", "Lcom/btelo/coding/notification/NotificationChannelManager;", "getNotificationChannelManager", "()Lcom/btelo/coding/notification/NotificationChannelManager;", "setNotificationChannelManager", "(Lcom/btelo/coding/notification/NotificationChannelManager;)V", "onCreate", "", "Companion", "app_debug"})
public final class BteloApp extends android.app.Application {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BteloApp";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope applicationScope = null;
    @javax.inject.Inject()
    public com.btelo.coding.notification.NotificationChannelManager notificationChannelManager;
    @javax.inject.Inject()
    public com.btelo.coding.push.FcmTokenManager fcmTokenManager;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.BteloApp.Companion Companion = null;
    
    public BteloApp() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.notification.NotificationChannelManager getNotificationChannelManager() {
        return null;
    }
    
    public final void setNotificationChannelManager(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.notification.NotificationChannelManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.push.FcmTokenManager getFcmTokenManager() {
        return null;
    }
    
    public final void setFcmTokenManager(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.push.FcmTokenManager p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/btelo/coding/BteloApp$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}