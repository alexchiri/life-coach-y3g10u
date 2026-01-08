package com.kroslabs.lifecoach.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.kroslabs.lifecoach.MainActivity
import com.kroslabs.lifecoach.R
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val CHANNEL_ID_REMINDERS = "life_coach_reminders"
    private const val CHANNEL_ID_MILESTONES = "life_coach_milestones"

    const val NOTIFICATION_ID_CHECKIN = 1001
    const val NOTIFICATION_ID_WEEKLY = 1002
    const val NOTIFICATION_ID_EXPERIMENT = 1003
    const val NOTIFICATION_ID_MILESTONE = 1004

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Reminders channel
        val remindersChannel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily check-in and weekly reflection reminders"
        }

        // Milestones channel
        val milestonesChannel = NotificationChannel(
            CHANNEL_ID_MILESTONES,
            "Milestones",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Celebration notifications for achievements"
        }

        notificationManager.createNotificationChannel(remindersChannel)
        notificationManager.createNotificationChannel(milestonesChannel)
    }

    fun showCheckInReminder(context: Context, experimentTitle: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to Check In")
            .setContentText("How's your experiment \"$experimentTitle\" going?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CHECKIN, notification)
    }

    fun showWeeklyReflectionReminder(context: Context) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Weekly Reflection Time")
            .setContentText("Take a moment to reflect on your progress this week")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WEEKLY, notification)
    }

    fun showExperimentLifecycleAlert(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPERIMENT, notification)
    }

    fun showMilestoneNotification(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MILESTONES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MILESTONE, notification)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(context)

        // Cancel any existing daily reminder
        workManager.cancelUniqueWork("daily_checkin_reminder")

        // Calculate initial delay to target time
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_checkin_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }

    fun scheduleWeeklyReminder(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Cancel any existing weekly reminder
        workManager.cancelUniqueWork("weekly_reflection_reminder")

        val weeklyWorkRequest = PeriodicWorkRequestBuilder<WeeklyReminderWorker>(
            7, TimeUnit.DAYS
        )
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "weekly_reflection_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            weeklyWorkRequest
        )
    }

    fun cancelAllReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("daily_checkin_reminder")
        workManager.cancelUniqueWork("weekly_reflection_reminder")
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        NotificationHelper.showCheckInReminder(applicationContext, "your experiment")
        return Result.success()
    }
}

class WeeklyReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        NotificationHelper.showWeeklyReflectionReminder(applicationContext)
        return Result.success()
    }
}
