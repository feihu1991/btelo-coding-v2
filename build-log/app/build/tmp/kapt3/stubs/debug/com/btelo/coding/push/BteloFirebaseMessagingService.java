package com.btelo.coding.push;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.btelo.coding.data.local.DataStoreManager;
import com.btelo.coding.notification.NotificationHelper;
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\r\b\u0007\u0018\u0000 \'2\u00020\u0001:\u0001\'B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u0016H\u0002J\b\u0010\u0017\u001a\u00020\u0012H\u0002J\u001c\u0010\u0018\u001a\u00020\u00122\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u0016H\u0002J\u0010\u0010\u0019\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u001bH\u0016J\u0010\u0010\u001c\u001a\u00020\u00122\u0006\u0010\u001d\u001a\u00020\u0014H\u0016J\u0010\u0010\u001e\u001a\u00020\u00122\u0006\u0010\u001d\u001a\u00020\u0014H\u0002J6\u0010\u001f\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\b\u0010 \u001a\u0004\u0018\u00010\u00142\u0006\u0010!\u001a\u00020\u00142\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u0016H\u0002J0\u0010\"\u001a\u00020\u00122\b\u0010#\u001a\u0004\u0018\u00010\u00142\b\u0010$\u001a\u0004\u0018\u00010\u00142\u0012\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00140\u0016H\u0002J\u0016\u0010%\u001a\u00020\u00122\u0006\u0010\u001d\u001a\u00020\u0014H\u0082@\u00a2\u0006\u0002\u0010&R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/btelo/coding/push/BteloFirebaseMessagingService;", "Lcom/google/firebase/messaging/FirebaseMessagingService;", "()V", "dataStoreManager", "Lcom/btelo/coding/data/local/DataStoreManager;", "getDataStoreManager", "()Lcom/btelo/coding/data/local/DataStoreManager;", "setDataStoreManager", "(Lcom/btelo/coding/data/local/DataStoreManager;)V", "notificationHelper", "Lcom/btelo/coding/notification/NotificationHelper;", "getNotificationHelper", "()Lcom/btelo/coding/notification/NotificationHelper;", "setNotificationHelper", "(Lcom/btelo/coding/notification/NotificationHelper;)V", "serviceScope", "Lkotlinx/coroutines/CoroutineScope;", "broadcastMessageToUi", "", "sessionId", "", "data", "", "broadcastSyncRequest", "handleDataMessage", "onMessageReceived", "remoteMessage", "Lcom/google/firebase/messaging/RemoteMessage;", "onNewToken", "token", "sendTokenRefreshBroadcast", "showChatNotification", "senderName", "content", "showNotification", "title", "body", "uploadFcmTokenToServer", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class BteloFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "BteloFCM";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_FCM_TOKEN_REFRESH = "com.btelo.coding.FCM_TOKEN_REFRESH";
    @javax.inject.Inject()
    public com.btelo.coding.data.local.DataStoreManager dataStoreManager;
    @javax.inject.Inject()
    public com.btelo.coding.notification.NotificationHelper notificationHelper;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope serviceScope = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.btelo.coding.push.BteloFirebaseMessagingService.Companion Companion = null;
    
    public BteloFirebaseMessagingService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.data.local.DataStoreManager getDataStoreManager() {
        return null;
    }
    
    public final void setDataStoreManager(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.data.local.DataStoreManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.btelo.coding.notification.NotificationHelper getNotificationHelper() {
        return null;
    }
    
    public final void setNotificationHelper(@org.jetbrains.annotations.NotNull()
    com.btelo.coding.notification.NotificationHelper p0) {
    }
    
    /**
     * Called when a new FCM token is generated.
     * This happens on first app start, token refresh, or app data clearing.
     */
    @java.lang.Override()
    public void onNewToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token) {
    }
    
    /**
     * Called when a message is received from FCM.
     * This handles both notification and data messages.
     */
    @java.lang.Override()
    public void onMessageReceived(@org.jetbrains.annotations.NotNull()
    com.google.firebase.messaging.RemoteMessage remoteMessage) {
    }
    
    /**
     * Handle data-only messages (silent push)
     */
    private final void handleDataMessage(java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    /**
     * Show notification for chat messages
     */
    private final void showChatNotification(java.lang.String sessionId, java.lang.String senderName, java.lang.String content, java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    /**
     * Show generic notification
     */
    private final void showNotification(java.lang.String title, java.lang.String body, java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    /**
     * Upload FCM token to server
     */
    private final java.lang.Object uploadFcmTokenToServer(java.lang.String token, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Send broadcast when token is refreshed
     */
    private final void sendTokenRefreshBroadcast(java.lang.String token) {
    }
    
    /**
     * Broadcast message to UI when app is in foreground
     */
    private final void broadcastMessageToUi(java.lang.String sessionId, java.util.Map<java.lang.String, java.lang.String> data) {
    }
    
    /**
     * Broadcast sync request
     */
    private final void broadcastSyncRequest() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/btelo/coding/push/BteloFirebaseMessagingService$Companion;", "", "()V", "ACTION_FCM_TOKEN_REFRESH", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}