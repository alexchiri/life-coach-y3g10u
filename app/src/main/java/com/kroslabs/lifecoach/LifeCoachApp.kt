package com.kroslabs.lifecoach

import android.app.Application
import com.kroslabs.lifecoach.notifications.NotificationHelper

class LifeCoachApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create notification channels
        NotificationHelper.createNotificationChannels(this)
    }
}
