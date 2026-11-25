package com.jaydorah.alzhimers

import android.app.Application
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.notifications.ReminderScheduler
import com.jaydorah.alzhimers.notifications.ReminderWorker

class YoAlzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPreferences.get(this)
        ReminderScheduler.createChannel(this)
        ReminderWorker.scheduleIfEnabled(this)
    }
}
