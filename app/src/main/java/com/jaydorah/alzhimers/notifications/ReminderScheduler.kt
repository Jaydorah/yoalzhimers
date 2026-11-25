package com.jaydorah.alzhimers.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jaydorah.alzhimers.MainActivity
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.Locales
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminder"
    const val WORK_NAME = "daily_streak_reminder"

    // Pre-Android 13 the app language lives in prefs and application contexts ignore it,
    // so resources are read through a locale-wrapped context. Inert on 13+ (system applies it).
    private fun localized(context: Context): Context =
        Locales.wrap(context, Locales.current(context))

    fun createChannel(rawContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = localized(rawContext)
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            val nm = rawContext.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun showReminder(rawContext: Context) {
        val context = localized(rawContext)
        val prefs = AppPreferences.get(context)
        if (!prefs.notificationsEnabled) return
        // The reminder exists to protect the streak; no need to nag once today's game is done.
        if (prefs.playedToday()) return

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val streak = prefs.streakForDisplay()
        val body = if (streak > 0) {
            context.getString(R.string.notif_body_streak, streak)
        } else {
            context.getString(R.string.notif_body_start)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun delayUntilNextReminder(): Long {
        val now = java.util.Calendar.getInstance()
        val target = (now.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private const val NOTIFICATION_ID = 1001
    private const val REMINDER_HOUR = 19

    fun initialDelayMs(): Long = delayUntilNextReminder().coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
}
