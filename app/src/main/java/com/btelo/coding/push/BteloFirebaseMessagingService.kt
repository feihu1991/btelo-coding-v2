package com.btelo.coding.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.btelo.coding.data.local.DataStoreManager
import com.btelo.coding.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BteloFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "BteloFCM"
        
        // Intent action for FCM token refresh
        const val ACTION_FCM_TOKEN_REFRESH = "com.btelo.coding.FCM_TOKEN_REFRESH"
    }

    @Inject
    lateinit var dataStoreManager: DataStoreManager
    
    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called when a new FCM token is generated.
     * This happens on first app start, token refresh, or app data clearing.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")
        
        // Save token locally
        serviceScope.launch {
            try {
                dataStoreManager.saveFcmToken(token)
                Log.d(TAG, "FCM token saved successfully")
                
                // Notify server about new token (if user is logged in)
                if (dataStoreManager.hasToken()) {
                    uploadFcmTokenToServer(token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token", e)
            }
        }
        
        // Broadcast token refresh for receivers
        sendTokenRefreshBroadcast(token)
    }

    /**
     * Called when a message is received from FCM.
     * This handles both notification and data messages.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
            
            // Show notification
            showNotification(notification.title, notification.body, remoteMessage.data)
        }
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }
    
    /**
     * Handle data-only messages (silent push)
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val messageType = data["type"]
        
        when (messageType) {
            "chat_message" -> {
                val sessionId = data["session_id"]
                val senderName = data["sender_name"]
                val content = data["content"]
                
                if (sessionId != null && content != null) {
                    // Check if app is in foreground
                    if (!notificationHelper.isAppInForeground()) {
                        // Show notification for background message
                        showChatNotification(sessionId, senderName, content, data)
                    } else {
                        // App is in foreground - broadcast to UI
                        broadcastMessageToUi(sessionId, data)
                    }
                }
            }
            "sync_request" -> {
                // Handle sync request from server
                Log.d(TAG, "Received sync request from server")
                broadcastSyncRequest()
            }
            else -> {
                Log.d(TAG, "Unknown message type: $messageType")
            }
        }
    }
    
    /**
     * Show notification for chat messages
     */
    private fun showChatNotification(
        sessionId: String,
        senderName: String?,
        content: String,
        data: Map<String, String>
    ) {
        // Check if notifications are enabled and not in DND period
        if (!notificationHelper.shouldShowNotification()) {
            Log.d(TAG, "Notification skipped - DND or notifications disabled")
            return
        }
        
        val title = senderName ?: "New Message"
        val body = content.take(100) // Limit body length
        
        notificationHelper.showChatNotification(
            sessionId = sessionId,
            title = title,
            body = body,
            data = data
        )
    }
    
    /**
     * Show generic notification
     */
    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        if (!notificationHelper.shouldShowNotification()) {
            return
        }
        
        val sessionId = data["session_id"]
        notificationHelper.showChatNotification(
            sessionId = sessionId ?: "default",
            title = title ?: "Notification",
            body = body ?: "",
            data = data
        )
    }
    
    /**
     * Upload FCM token to server
     */
    private suspend fun uploadFcmTokenToServer(token: String) {
        try {
            // TODO: Implement server API call to register FCM token
            Log.d(TAG, "Uploading FCM token to server: $token")
            // Example API call structure:
            // val response = syncApi.registerFcmToken(token)
            // Log.d(TAG, "Token upload response: ${response.code()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload FCM token", e)
        }
    }
    
    /**
     * Send broadcast when token is refreshed
     */
    private fun sendTokenRefreshBroadcast(token: String) {
        val intent = android.content.Intent(ACTION_FCM_TOKEN_REFRESH).apply {
            putExtra("token", token)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Broadcast message to UI when app is in foreground
     */
    private fun broadcastMessageToUi(sessionId: String, data: Map<String, String>) {
        val intent = android.content.Intent("com.btelo.coding.NEW_MESSAGE").apply {
            putExtra("session_id", sessionId)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
    
    /**
     * Broadcast sync request
     */
    private fun broadcastSyncRequest() {
        val intent = android.content.Intent("com.btelo.coding.SYNC_REQUEST").apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
