package com.btelo.coding.notification

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.btelo.coding.MainActivity
import com.btelo.coding.R
import com.btelo.coding.data.local.DataStoreManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for creating and managing notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val TAG = "NotificationHelper"
        
        // Notification channel IDs
        const val CHANNEL_ID_CHAT = "chat_messages"
        const val CHANNEL_ID_SYNC = "sync_status"
        const val CHANNEL_ID_GENERAL = "general"
        
        // Notification IDs
        const val NOTIFICATION_ID_CHAT_BASE = 1000
        const val NOTIFICATION_ID_SYNC = 2000
        
        // Intent extras
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    }

    /**
     * Check if the app is currently in the foreground
     */
    fun isAppInForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        
        val packageName = context.packageName
        return appProcesses.any { appProcess ->
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
            appProcess.processName == packageName
        }
    }

    /**
     * Check if notification should be shown (considers DND settings)
     */
    fun shouldShowNotification(): Boolean {
        // Check if notifications are enabled
        if (!dataStoreManager.isNotificationEnabled()) {
            Log.d(TAG, "Notifications are disabled")
            return false
        }
        
        // Check DND period
        if (isInDoNotDisturbPeriod()) {
            Log.d(TAG, "Currently in Do Not Disturb period")
            return false
        }
        
        return true
    }

    /**
     * Check if current time is within Do Not Disturb period
     */
    private fun isInDoNotDisturbPeriod(): Boolean {
        val dndEnabled = dataStoreManager.isDndEnabled()
        if (!dndEnabled) return false
        
        val startHour = dataStoreManager.getDndStartHour()
        val startMinute = dataStoreManager.getDndStartMinute()
        val endHour = dataStoreManager.getDndEndHour()
        val endMinute = dataStoreManager.getDndEndMinute()
        
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute
        
        return if (startTime <= endTime) {
            // Same day DND (e.g., 22:00 - 07:00 would be handled differently)
            currentTime in startTime..endTime
        } else {
            // Overnight DND (e.g., 22:00 - 07:00)
            currentTime >= startTime || currentTime <= endTime
        }
    }

    /**
     * Show a chat notification
     */
    fun showChatNotification(
        sessionId: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent for clicking notification
        val intent = createNotificationIntent(sessionId, "chat", data)
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                // Sound
                if (dataStoreManager.isNotificationSoundEnabled()) {
                    val soundUri = android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_NOTIFICATION
                    )
                    setSound(soundUri)
                }
                
                // Vibration
                if (dataStoreManager.isNotificationVibrationEnabled()) {
                    val vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setVibrate(vibrationPattern)
                }
                
                // LED
                if (dataStoreManager.isNotificationLedEnabled()) {
                    setLights(
                        android.graphics.Color.BLUE,
                        1000,
                        1000
                    )
                }
            }
            .build()
        
        // Use sessionId hashCode for unique notification per session
        val notificationId = NOTIFICATION_ID_CHAT_BASE + sessionId.hashCode()
        notificationManager.notify(notificationId, notification)
        
        Log.d(TAG, "Chat notification shown for session: $sessionId")
    }

    /**
     * Show a sync status notification
     */
    fun showSyncNotification(title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_SYNC, notification)
    }

    /**
     * Cancel sync notification
     */
    fun cancelSyncNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_SYNC)
    }

    /**
     * Cancel all chat notifications for a session
     */
    fun cancelChatNotification(sessionId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = NOTIFICATION_ID_CHAT_BASE + sessionId.hashCode()
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    /**
     * Create intent for notification click
     */
    private fun createNotificationIntent(
        sessionId: String,
        type: String,
        data: Map<String, String>
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_NOTIFICATION_TYPE, type)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }
}
