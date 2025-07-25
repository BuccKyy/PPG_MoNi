package com.example.ppg_moni.services

import android.content.Context
import androidx.work.*
import com.example.ppg_moni.notifications.HealthNotificationManager
import java.util.concurrent.TimeUnit

class HealthReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val notificationManager = HealthNotificationManager(applicationContext)
            notificationManager.sendMeasurementReminder()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "health_reminder_work"
        
        fun schedulePeriodicReminder(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()

            val reminderRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
                1, TimeUnit.DAYS // Daily reminder
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS) // First reminder after 1 hour
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
        }
        
        fun cancelReminder(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
} 