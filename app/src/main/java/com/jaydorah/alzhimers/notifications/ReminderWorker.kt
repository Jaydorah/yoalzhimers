package com.jaydorah.alzhimers.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jaydorah.alzhimers.core.AppPreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ReminderScheduler.showReminder(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(ReminderScheduler.initialDelayMs(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ReminderScheduler.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(ReminderScheduler.WORK_NAME)
        }

        fun scheduleIfEnabled(context: Context) {
            if (AppPreferences.get(context).notificationsEnabled) schedule(context) else cancel(context)
        }
    }
}
