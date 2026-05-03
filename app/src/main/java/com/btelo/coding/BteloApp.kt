package com.btelo.coding

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.btelo.coding.data.update.UpdatePreparationWorker
import com.btelo.coding.notification.NotificationChannelManager
import com.btelo.coding.push.FcmTokenManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BteloApp : Application() {
    
    companion object {
        private const val TAG = "BteloApp"
        private const val UPDATE_WORK_NAME = "auto_update_preparation"
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager
    
    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BteloApp onCreate")
        
        // Initialize notification channels
        applicationScope.launch {
            try {
                notificationChannelManager.createNotificationChannels()
                Log.d(TAG, "Notification channels initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification channels", e)
            }
        }
        
        // Refresh FCM token if needed
        applicationScope.launch {
            try {
                if (fcmTokenManager.isFcmAvailable()) {
                    val token = fcmTokenManager.getToken()
                    Log.d(TAG, "FCM token retrieved: ${token?.take(20)}...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }

        scheduleUpdatePreparation()
    }

    private fun scheduleUpdatePreparation() {
        val workRequest = PeriodicWorkRequestBuilder<UpdatePreparationWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
