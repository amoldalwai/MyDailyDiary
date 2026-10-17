package com.amoldalwai.diary.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.amoldalwai.diary.MainActivity
import com.amoldalwai.diary.R
import com.amoldalwai.diary.data.DiaryRepository
import com.amoldalwai.diary.data.SettingsStore
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {
    private const val CHANNEL_ID = "daily_reminder_v2"
    private const val LEGACY_CHANNEL_ID = "daily_reminder"
    private const val REQUEST_ALARM = 1001
    private const val REQUEST_OPEN = 1002

    private val messages = listOf(
        "How was your day? Take a minute to write it down.",
        "Capture today before it slips away.",
        "A few words now will be a treasured memory later.",
        "What made you smile today?",
    )

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // The first channel was created with default importance; importance can't be raised later, so it was replaced.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A nudge to write your diary entry"
            },
        )
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        var at = LocalDate.now().atTime(hour, minute).atZone(ZoneId.systemDefault())
        if (!at.isAfter(ZonedDateTime.now())) at = at.plusDays(1)
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), alarmIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    fun notifyNow(context: Context) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val message = messages.random()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Time for your diary")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(REQUEST_ALARM, notification) }
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsStore(context)
        if (!settings.reminderEnabled) return
        if (!DiaryRepository(context).hasEntryOn(LocalDate.now().toEpochDay())) {
            ReminderScheduler.notifyNow(context)
        }
        ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context)
        if (settings.reminderEnabled) {
            ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
        }
    }
}
