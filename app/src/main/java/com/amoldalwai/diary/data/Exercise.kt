package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max

/** A muscle the user trains. [icon] is a muscle picture key (`muscle_chest`, ...) or an icon catalog key; [target] is days per week. */
data class Muscle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val target: Int = 1,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Everything stored for the exercise section. [logs] are (muscle id, day) pairs; [restDays] are days marked as rest. */
data class ExerciseData(
    val muscles: List<Muscle> = emptyList(),
    val logs: Set<Pair<String, Long>> = emptySet(),
    val restDays: Set<Long> = emptySet(),
)

class ExerciseRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase

    suspend fun load(): ExerciseData = withContext(Dispatchers.IO) {
        val muscles = buildList {
            db.rawQuery("SELECT id, name, icon, target, position, created_at FROM muscles WHERE deleted = 0 ORDER BY position, created_at", null).use {
                while (it.moveToNext()) add(Muscle(it.getString(0), it.getString(1), it.getString(2), it.getInt(3), it.getInt(4), it.getLong(5)))
            }
        }
        val ids = muscles.map { it.id }.toSet()
        val logs = buildSet {
            db.rawQuery("SELECT muscle_id, date_epoch FROM muscle_logs", null).use {
                while (it.moveToNext()) {
                    val id = it.getString(0)
                    if (id in ids) add(id to it.getLong(1))
                }
            }
        }
        val rest = buildSet {
            db.rawQuery("SELECT date_epoch FROM rest_days", null).use { while (it.moveToNext()) add(it.getLong(0)) }
        }
        ExerciseData(muscles, logs, rest)
    }

    suspend fun save(muscle: Muscle) = withContext(Dispatchers.IO) {
        db.insertWithOnConflict(
            "muscles",
            null,
            ContentValues().apply {
                put("id", muscle.id)
                put("name", muscle.name)
                put("icon", muscle.icon)
                put("target", muscle.target)
                put("position", muscle.position)
                put("created_at", muscle.createdAt)
                put("deleted", 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    /** Hides the muscle but keeps its history, so adding it back later restores everything. */
    suspend fun remove(id: String) = withContext(Dispatchers.IO) { db.execSQL("UPDATE muscles SET deleted = 1 WHERE id = ?", arrayOf<Any?>(id)) }

    /** Brings back a removed muscle with the same name, history included; false when there was none. */
    suspend fun restore(name: String): Boolean = withContext(Dispatchers.IO) {
        db.update("muscles", ContentValues().apply { put("deleted", 0) }, "deleted = 1 AND lower(name) = lower(?)", arrayOf(name)) > 0
    }

    suspend fun setTrained(muscleId: String, epochDay: Long, trained: Boolean) = withContext(Dispatchers.IO) {
        if (trained) {
            db.insertWithOnConflict(
                "muscle_logs",
                null,
                ContentValues().apply {
                    put("muscle_id", muscleId)
                    put("date_epoch", epochDay)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            db.delete("rest_days", "date_epoch = ?", arrayOf(epochDay.toString()))
        } else {
            db.delete("muscle_logs", "muscle_id = ? AND date_epoch = ?", arrayOf(muscleId, epochDay.toString()))
        }
    }

    suspend fun setRest(epochDay: Long, rest: Boolean) = withContext(Dispatchers.IO) {
        if (rest) {
            db.insertWithOnConflict("rest_days", null, ContentValues().apply { put("date_epoch", epochDay) }, SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            db.delete("rest_days", "date_epoch = ?", arrayOf(epochDay.toString()))
        }
    }
}

/** One muscle's numbers for the exercise screens. */
data class MuscleStat(
    val muscle: Muscle,
    /** Days trained in the current week (Monday to Sunday). */
    val thisWeek: Int,
    /** Weeks in a row that reached [Muscle.target]. */
    val streakWeeks: Int,
    /** Days since it was last trained; null when it never was. */
    val daysSince: Int?,
    /** Days trained per week over the last eight weeks, oldest first. */
    val history: List<Int>,
    /** Which days of the current week it was trained on (Monday first). */
    val weekDays: List<Boolean>,
)

data class ExerciseStats(
    val muscles: List<MuscleStat>,
    val weeklyGoal: Int,
    val streakWeeks: Int,
    val bestStreakWeeks: Int,
    /** Workout days (any muscle) this week, and per week over the last twelve weeks, oldest first. */
    val workoutsThisWeek: Int,
    val weeklyWorkouts: List<Int>,
    val workoutsThisMonth: Int,
    /** Days since the last workout day; null when there has been none. */
    val daysSinceWorkout: Int?,
    /** For Monday to Sunday of this week: workout, rest, or neither. */
    val weekWorkout: List<Boolean>,
    val weekRest: List<Boolean>,
    /** Muscles trained on each day of this week (Monday first). */
    val weekMuscleCounts: List<Int>,
)

object ExerciseMath {
    /** Number of the Monday-to-Sunday week a day belongs to; 1970-01-05 was a Monday. */
    fun weekOf(epochDay: Long): Long = Math.floorDiv(epochDay + 3, 7L)

    fun monday(epochDay: Long): Long = weekOf(epochDay) * 7 - 3

    /** Consecutive weeks meeting [goal] up to now; a current week that hasn't reached it yet doesn't break the run. */
    private fun streak(counts: Map<Long, Int>, goal: Int, currentWeek: Long): Int {
        var week = currentWeek
        var n = 0
        if ((counts[week] ?: 0) >= goal) n++
        week--
        while ((counts[week] ?: 0) >= goal) {
            n++
            week--
        }
        return n
    }

    private fun best(counts: Map<Long, Int>, goal: Int, currentWeek: Long): Int {
        val first = counts.keys.minOrNull() ?: return 0
        var best = 0
        var run = 0
        for (w in first..currentWeek) {
            if ((counts[w] ?: 0) >= goal) {
                run++
                best = max(best, run)
            } else {
                run = 0
            }
        }
        return best
    }

    fun stats(data: ExerciseData, weeklyGoal: Int, today: LocalDate): ExerciseStats {
        val t = today.toEpochDay()
        val thisWeek = weekOf(t)
        val monday = monday(t)
        val goal = weeklyGoal.coerceAtLeast(1)

        val daysByMuscle = data.logs.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        val anyDays = data.logs.map { it.second }.toSet()

        fun weekCounts(days: Set<Long>): Map<Long, Int> = days.groupingBy { weekOf(it) }.eachCount()

        val overall = weekCounts(anyDays)
        val monthStart = today.withDayOfMonth(1).toEpochDay()
        val muscleStats = data.muscles.map { muscle ->
            val days = daysByMuscle[muscle.id].orEmpty()
            val counts = weekCounts(days)
            val target = muscle.target.coerceAtLeast(1)
            MuscleStat(
                muscle = muscle,
                thisWeek = counts[thisWeek] ?: 0,
                streakWeeks = streak(counts, target, thisWeek),
                daysSince = days.filter { it <= t }.maxOrNull()?.let { (t - it).toInt() },
                history = (7 downTo 0).map { counts[thisWeek - it] ?: 0 },
                weekDays = (0..6).map { (monday + it) in days },
            )
        }
        return ExerciseStats(
            muscles = muscleStats,
            weeklyGoal = goal,
            streakWeeks = streak(overall, goal, thisWeek),
            bestStreakWeeks = best(overall, goal, thisWeek),
            workoutsThisWeek = overall[thisWeek] ?: 0,
            weeklyWorkouts = (11 downTo 0).map { overall[thisWeek - it] ?: 0 },
            workoutsThisMonth = anyDays.count { it in monthStart..t },
            daysSinceWorkout = anyDays.filter { it <= t }.maxOrNull()?.let { (t - it).toInt() },
            weekWorkout = (0..6).map { (monday + it) in anyDays },
            weekRest = (0..6).map { (monday + it) in data.restDays },
            weekMuscleCounts = (0..6).map { d -> data.logs.count { it.second == monday + d } },
        )
    }

    /** The muscles offered the first time the section opens: name, picture key, weekly target. */
    val defaults = listOf(
        Triple("Chest", "muscle_chest", 2), Triple("Shoulders", "muscle_shoulders", 2), Triple("Legs", "muscle_legs", 1),
        Triple("Triceps", "muscle_triceps", 1), Triple("Biceps", "muscle_biceps", 2), Triple("Abs", "muscle_abs", 3),
        Triple("Back", "muscle_back", 1), Triple("Forearms", "muscle_forearms", 1), Triple("Glutes", "muscle_glutes", 1),
    )

    /** Muscles with no picture yet get a catalog icon. */
    val suggestions = listOf(
        "Calves" to "walk", "Traps" to "gym", "Lats" to "gym", "Quads" to "run",
        "Hamstrings" to "run", "Neck" to "heart", "Cardio" to "heart", "Core" to "gym",
    )
}
