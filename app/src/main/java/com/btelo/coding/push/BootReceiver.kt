package com.btelo.coding.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.btelo.coding.notification.NotificationChannelManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receiver for device boot to reinitialize FCM
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, initializing...")
            
            // Re-create notification channels
            notificationChannelManager.createNotificationChannels()
            
            // TODO: Re-initialize FCM token if needed
            Log.d(TAG, "Boot initialization complete")
        }
    }
}
