package com.btelo.coding.push

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token retrieval and operations
 */
@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Get the current FCM registration token.
     * Returns null if token retrieval fails.
     */
    suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token retrieved: $token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Get token synchronously (blocking call, avoid on main thread)
     * 警告：此方法是阻塞调用，不要在主线程调用
     */
    @WorkerThread
    fun getTokenSync(): String? {
        return try {
            // Use Tasks.await for synchronous access
            // This is acceptable for startup scenarios but avoid on main thread
            com.google.android.gms.tasks.Tasks.await(FirebaseMessaging.getInstance().token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token synchronously", e)
            null
        }
    }

    /**
     * Delete the current FCM token.
     * This is typically called on logout to stop receiving notifications for that user.
     */
    suspend fun deleteToken() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
            Log.d(TAG, "FCM Token deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete FCM token", e)
        }
    }

    /**
     * Subscribe to a topic for group messaging.
     * Useful for sending notifications to specific user groups.
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            false
        }
    }

    /**
     * Unsubscribe from a topic.
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            false
        }
    }

    /**
     * Check if FCM is available on this device.
     * For simplicity, we assume FCM is always available on valid Android devices
     * with Google Play Services installed (which is required for Firebase).
     */
    fun isFcmAvailable(): Boolean {
        return try {
            // Simple availability check - if FirebaseMessaging works, FCM is available
            FirebaseMessaging.getInstance().isAutoInitEnabled
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking FCM availability", e)
            false
        }
    }
}
