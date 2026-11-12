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
import com.amoldalwai.diary.data.HabitsRepository
import com.amoldalwai.diary.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Asks about one unfinished habit at a time, spaced through the day, with a one-tap Done button. */
object HabitNudges {
    private const val CHANNEL_ID = "habit_nudges_v2"
    private const val LEGACY_CHANNEL_ID = "habit_nudges"
    private const val REQUEST_ALARM = 2001
    private const val REQUEST_OPEN = 2002
    private const val NOTIFICATION_ID = 2003
    const val ACTION_DONE = "com.amoldalwai.diary.HABIT_DONE"
    const val EXTRA_HABIT = "habit"

    // %h = habit name, %s = current streak, %n = the day number a tick would reach.
    private val streakTexts = listOf(
        "Don't break your %s-day streak of %h 🔥",
        "%s days of %h in a row. Make it %n today?",
        "Your %h streak is at %s. Keep the chain alive!",
        "Day %n of %h is waiting for you.",
        "%s days strong. Don't let %h slip today.",
        "Streak check: %h, day %n. Still on?",
    )
    private val plainTexts = listOf(
        "Did you complete %h today?",
        "Quick check: have you done %h yet?",
        "Psst… %h still needs a tick today.",
        "Small step, big change: time for %h?",
        "Future you will thank you for %h.",
        "Has %h happened yet? Tap Done when it has.",
        "Ready for %h? You've got this 💪",
        "Let's not leave %h for tomorrow.",
        "A tiny win is waiting: %h.",
    )

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // The first channel was created with default importance, which only lands quietly in the shade; importance can't be raised later.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Habit nudges", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A question about one habit at a time"
            },
        )
    }

    /** Sets the next alarm: one gap from now, kept inside the daily window, else the start of the next window. */
    fun schedule(context: Context, settings: SettingsStore) {
        val windows = listOf(settings.nudgeFrom to settings.nudgeTo) +
            settings.nudgeOverrides.values.filter { it.on && it.from >= 0 && it.to > it.from }.map { it.from to it.to }
        val start = windows.minOf { it.first }
        val end = windows.maxOf { it.second }
        val now = ZonedDateTime.now()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val nowMinutes = now.hour * 60 + now.minute
        val at = when {
            nowMinutes < start -> today.atTime(start / 60, start % 60).atZone(zone)
            else -> {
                val candidate = now.plusMinutes(settings.nudgeGap.toLong())
                val minutes = candidate.hour * 60 + candidate.minute
                if (candidate.toLocalDate() == today && minutes in start..end) candidate
                else today.plusDays(1).atTime(start / 60, start % 60).atZone(zone)
            }
        }
        settings.nudgeNextAt = at.toInstant().toEpochMilli()
        arm(context, settings.nudgeNextAt)
    }

    private fun arm(context: Context, atMillis: Long) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent(context))
    }

    /**
     * An app update or a force-stop wipes alarms. Put the next one back at its saved time, or pick a new time when that has passed,
     * so opening the app doesn't keep pushing the next nudge further away.
     */
    fun ensureScheduled(context: Context, settings: SettingsStore) {
        val next = settings.nudgeNextAt
        if (next > System.currentTimeMillis()) arm(context, next) else schedule(context, settings)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** Posts a nudge for one unfinished habit; returns false when there was nothing to ask about. [ignoreDone] is for the test button. */
    suspend fun nudge(context: Context, settings: SettingsStore, ignoreWindow: Boolean = false, ignoreDone: Boolean = false): Boolean {
        val habitsRepo = HabitsRepository(context)
        val today = LocalDate.now()
        val done = habitsRepo.doneIds(today.toEpochDay())
        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val pending = habitsRepo.habits().filter { habit ->
            val n = settings.nudgeFor(habit.id)
            val from = if (n.from >= 0) n.from else settings.nudgeFrom
            val to = if (n.to >= 0) n.to else settings.nudgeTo
            (ignoreDone || habit.id !in done) && (ignoreWindow || (n.on && nowMinutes in from..to))
        }
        if (pending.isEmpty()) return false

        // Rotate through the habits instead of asking about the same one every time.
        val last = settings.nudgeLastHabit
        val lastIndex = pending.indexOfFirst { it.id == last }
        val habit = pending[(lastIndex + 1).coerceAtLeast(0) % pending.size]
        val streak = habitsRepo.stats(today).firstOrNull { it.habit.id == habit.id }?.currentStreak ?: 0

        val pool = if (streak > 0) streakTexts else plainTexts
        var index = pool.indices.random()
        if (pool.size > 1 && streak == 0 && index == settings.nudgeLastText) index = (index + 1) % pool.size
        settings.nudgeLastHabit = habit.id
        settings.nudgeLastText = index
        val text = pool[index].replace("%h", habit.name).replace("%s", streak.toString()).replace("%n", (streak + 1).toString())

        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val doneIntent = PendingIntent.getBroadcast(
            context,
            habit.id.hashCode(),
            Intent(context, HabitNudgeReceiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_HABIT, habit.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(habit.name)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .addAction(R.drawable.ic_launcher_monochrome, "Done ✓", doneIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        // One at a time: a new nudge replaces an unanswered one.
        runCatching { context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification) }
        return true
    }

    suspend fun markDone(context: Context, habitId: String) {
        HabitsRepository(context).setDone(habitId, LocalDate.now().toEpochDay(), true)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, HabitNudgeReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class HabitNudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsStore(context)
                if (intent.action == HabitNudges.ACTION_DONE) {
                    intent.getStringExtra(HabitNudges.EXTRA_HABIT)?.let { HabitNudges.markDone(context, it) }
                } else if (settings.nudgesEnabled) {
                    HabitNudges.nudge(context, settings)
                    HabitNudges.schedule(context, settings)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
