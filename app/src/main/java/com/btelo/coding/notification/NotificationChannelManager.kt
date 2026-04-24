package com.btelo.coding.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channels for Android O and above
 */
@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationChannelMgr"
        
        // Channel descriptions
        private const val CHAT_CHANNEL_DESC = "Chat message notifications"
        private const val SYNC_CHANNEL_DESC = "Sync status notifications"
        private const val GENERAL_CHANNEL_DESC = "General app notifications"
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Create all notification channels required by the app
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChatChannel()
            createSyncChannel()
            createGeneralChannel()
            Log.d(TAG, "All notification channels created")
        }
    }

    /**
     * Create chat messages notification channel
     */
    private fun createChatChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHAT_CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Chat notification channel created")
        }
    }

    /**
     * Create sync status notification channel
     */
    private fun createSyncChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID_SYNC,
                "Sync Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SYNC_CHANNEL_DESC
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Sync notification channel created")
        }
    }

    /**
     * Create general notifications channel
     */
    private fun createGeneralChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationHelper.CHANNEL_ID_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = GENERAL_CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "General notification channel created")
        }
    }

    /**
     * Update chat channel settings
     */
    fun updateChatChannelSettings(
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        ledEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID_CHAT)
            channel?.apply {
                if (soundEnabled) {
                    setSound(
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                        null
                    )
                } else {
                    setSound(null, null)
                }
                
                if (vibrationEnabled) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                } else {
                    enableVibration(false)
                    vibrationPattern = null
                }
                
                enableLights(ledEnabled)
                
                notificationManager.createNotificationChannel(this)
                Log.d(TAG, "Chat channel settings updated")
            }
        }
    }

    /**
     * Delete all notification channels
     */
    fun deleteAllChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(NotificationHelper.CHANNEL_ID_CHAT)
            notificationManager.deleteNotificationChannel(NotificationHelper.CHANNEL_ID_SYNC)
            notificationManager.deleteNotificationChannel(NotificationHelper.CHANNEL_ID_GENERAL)
            Log.d(TAG, "All notification channels deleted")
        }
    }
}
