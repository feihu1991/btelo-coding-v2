package com.btelo.coding.data.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class UpdatePreparationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateWorkerEntryPoint {
        fun updateManager(): AppUpdateManager
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            UpdateWorkerEntryPoint::class.java
        )
        val updateManager = entryPoint.updateManager()

        return runCatching {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.Available -> {
                    updateManager.prepareUpdateInBackground(result.info)
                    Result.success()
                }

                UpdateCheckResult.NotAvailable -> {
                    updateManager.clearPendingUpdate()
                    Result.success()
                }
            }
        }.getOrElse {
            Result.retry()
        }
    }
}
