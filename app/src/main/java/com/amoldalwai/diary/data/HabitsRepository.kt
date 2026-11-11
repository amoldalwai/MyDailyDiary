package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.max

/** A habit with all its day ticks; the unit that is synced to Drive as one file. */
data class HabitRecord(val habit: Habit, val checks: Map<Long, HabitCheck>) {
    val maxUpdated: Long get() = max(habit.updatedAt, checks.values.maxOfOrNull { it.updatedAt } ?: 0L)
}

class HabitsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase

    suspend fun habits(): List<Habit> = withContext(Dispatchers.IO) {
        buildList {
            db.rawQuery(
                "SELECT id, name, emoji, position, created_at, updated_at FROM habits WHERE deleted = 0 ORDER BY position, created_at",
                null,
            ).use {
                while (it.moveToNext()) {
                    add(Habit(it.getString(0), it.getString(1), it.getString(2), it.getInt(3), it.getLong(4), it.getLong(5)))
                }
            }
        }
    }

    suspend fun save(habit: Habit) = withContext(Dispatchers.IO) { upsertHabit(habit) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        db.execSQL("UPDATE habits SET deleted = 1, updated_at = ? WHERE id = ?", arrayOf<Any?>(System.currentTimeMillis(), id))
    }

    suspend fun doneIds(epochDay: Long): Set<String> = withContext(Dispatchers.IO) {
        buildSet {
            db.rawQuery(
                "SELECT c.habit_id FROM habit_checks c JOIN habits h ON h.id = c.habit_id WHERE c.date_epoch = ? AND c.done = 1 AND h.deleted = 0",
                arrayOf(epochDay.toString()),
            ).use { while (it.moveToNext()) add(it.getString(0)) }
        }
    }

    suspend fun setDone(habitId: String, epochDay: Long, done: Boolean) = withContext(Dispatchers.IO) {
        upsertCheck(habitId, epochDay, HabitCheck(done, System.currentTimeMillis()))
    }

    suspend fun stats(today: LocalDate): List<HabitStat> = withContext(Dispatchers.IO) {
        val habits = buildList {
            db.rawQuery(
                "SELECT id, name, emoji, position, created_at, updated_at FROM habits WHERE deleted = 0 ORDER BY position, created_at",
                null,
            ).use {
                while (it.moveToNext()) {
                    add(Habit(it.getString(0), it.getString(1), it.getString(2), it.getInt(3), it.getLong(4), it.getLong(5)))
                }
            }
        }
        val days = HashMap<String, MutableSet<Long>>()
        db.rawQuery(
            "SELECT c.habit_id, c.date_epoch FROM habit_checks c JOIN habits h ON h.id = c.habit_id WHERE c.done = 1 AND h.deleted = 0",
            null,
        ).use { while (it.moveToNext()) days.getOrPut(it.getString(0)) { mutableSetOf() }.add(it.getLong(1)) }

        val t = today.toEpochDay()
        habits.map { habit ->
            val done = days[habit.id].orEmpty()
            var streak = 0
            var d = if (t in done) t else t - 1
            while (d in done) {
                streak++
                d--
            }
            var best = 0
            var run = 0
            var prev = Long.MIN_VALUE
            for (day in done.sorted()) {
                run = if (day == prev + 1) run + 1 else 1
                best = max(best, run)
                prev = day
            }
            HabitStat(
                habit = habit,
                currentStreak = streak,
                bestStreak = best,
                doneLast30 = done.count { it in (t - 29)..t },
                totalDone = done.size,
                last28 = (27 downTo 0).map { (t - it) in done },
            )
        }
    }

    // ---- Sync ----

    suspend fun allRecords(): List<HabitRecord> = withContext(Dispatchers.IO) {
        val habits = buildList {
            db.rawQuery("SELECT id, name, emoji, position, created_at, updated_at, deleted FROM habits", null).use {
                while (it.moveToNext()) {
                    add(Habit(it.getString(0), it.getString(1), it.getString(2), it.getInt(3), it.getLong(4), it.getLong(5), it.getInt(6) == 1))
                }
            }
        }
        val checks = HashMap<String, MutableMap<Long, HabitCheck>>()
        db.rawQuery("SELECT habit_id, date_epoch, done, updated_at FROM habit_checks", null).use {
            while (it.moveToNext()) {
                checks.getOrPut(it.getString(0)) { mutableMapOf() }[it.getLong(1)] = HabitCheck(it.getInt(2) == 1, it.getLong(3))
            }
        }
        habits.map { HabitRecord(it, checks[it.id].orEmpty()) }
    }

    suspend fun applyRecord(record: HabitRecord) = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            upsertHabit(record.habit)
            record.checks.forEach { (day, check) -> upsertCheck(record.habit.id, day, check) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun upsertHabit(h: Habit) {
        db.insertWithOnConflict(
            "habits",
            null,
            ContentValues().apply {
                put("id", h.id)
                put("name", h.name)
                put("emoji", h.emoji)
                put("position", h.position)
                put("created_at", h.createdAt)
                put("updated_at", h.updatedAt)
                put("deleted", if (h.deleted) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun upsertCheck(habitId: String, day: Long, check: HabitCheck) {
        db.insertWithOnConflict(
            "habit_checks",
            null,
            ContentValues().apply {
                put("habit_id", habitId)
                put("date_epoch", day)
                put("done", if (check.done) 1 else 0)
                put("updated_at", check.updatedAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}
