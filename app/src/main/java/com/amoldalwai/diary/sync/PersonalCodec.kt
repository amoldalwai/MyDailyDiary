package com.amoldalwai.diary.sync

import com.amoldalwai.diary.data.Book
import com.amoldalwai.diary.data.BookLog
import com.amoldalwai.diary.data.BookRecord
import com.amoldalwai.diary.data.BookStatus
import com.amoldalwai.diary.data.Game
import com.amoldalwai.diary.data.GameStatus
import com.amoldalwai.diary.data.WatchItem
import com.amoldalwai.diary.data.WatchKind
import com.amoldalwai.diary.data.WatchStatus
import com.amoldalwai.diary.data.Habit
import com.amoldalwai.diary.data.HabitCheck
import com.amoldalwai.diary.data.HabitRecord
import org.json.JSONArray
import org.json.JSONObject

/** JSON forms of habits and books for the Drive app folder, plus the merge rules for two copies. */
object PersonalCodec {

    fun encode(r: HabitRecord): String = JSONObject().apply {
        put("id", r.habit.id)
        put("name", r.habit.name)
        put("emoji", r.habit.emoji)
        put("position", r.habit.position)
        put("createdAt", r.habit.createdAt)
        put("updatedAt", r.habit.updatedAt)
        put("deleted", r.habit.deleted)
        put(
            "checks",
            JSONArray().apply {
                r.checks.forEach { (day, c) -> put(JSONArray().put(day).put(if (c.done) 1 else 0).put(c.updatedAt)) }
            },
        )
    }.toString()

    fun decodeHabit(json: String): HabitRecord {
        val o = JSONObject(json)
        val habit = Habit(
            id = o.getString("id"),
            name = o.getString("name"),
            emoji = o.getString("emoji"),
            position = o.optInt("position"),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
            deleted = o.optBoolean("deleted"),
        )
        val array = o.optJSONArray("checks") ?: JSONArray()
        val checks = buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONArray(i)
                put(item.getLong(0), HabitCheck(item.getInt(1) == 1, item.getLong(2)))
            }
        }
        return HabitRecord(habit, checks)
    }

    fun merge(a: HabitRecord, b: HabitRecord): HabitRecord {
        val habit = if (a.habit.updatedAt >= b.habit.updatedAt) a.habit else b.habit
        val checks = HashMap(a.checks)
        b.checks.forEach { (day, check) ->
            val mine = checks[day]
            if (mine == null || check.updatedAt > mine.updatedAt) checks[day] = check
        }
        return HabitRecord(habit, checks)
    }

    fun encode(r: BookRecord): String = JSONObject().apply {
        val b = r.book
        put("id", b.id)
        put("olKey", b.openLibraryKey ?: JSONObject.NULL)
        put("title", b.title)
        put("authors", b.authors)
        put("year", b.year ?: JSONObject.NULL)
        put("cover", b.cover ?: JSONObject.NULL)
        put("totalPages", b.totalPages ?: JSONObject.NULL)
        put("currentPage", b.currentPage)
        put("status", b.status.name)
        put("startedAt", b.startedAt ?: JSONObject.NULL)
        put("finishedAt", b.finishedAt ?: JSONObject.NULL)
        put("rating", b.rating ?: JSONObject.NULL)
        put("review", b.review)
        put("updatedAt", b.updatedAt)
        put("deleted", b.deleted)
        put(
            "logs",
            JSONArray().apply {
                r.logs.forEach { (day, l) -> put(JSONArray().put(day).put(l.page).put(l.updatedAt)) }
            },
        )
    }.toString()

    fun decodeBook(json: String): BookRecord {
        val o = JSONObject(json)
        fun optString(key: String) = if (o.isNull(key)) null else o.getString(key)
        fun optInt(key: String) = if (o.isNull(key)) null else o.getInt(key)
        fun optLong(key: String) = if (o.isNull(key)) null else o.getLong(key)
        val book = Book(
            id = o.getString("id"),
            openLibraryKey = optString("olKey"),
            title = o.getString("title"),
            authors = o.optString("authors"),
            year = optInt("year"),
            cover = optString("cover"),
            totalPages = optInt("totalPages"),
            currentPage = o.optInt("currentPage"),
            status = BookStatus.entries.firstOrNull { it.name == o.optString("status") } ?: BookStatus.Reading,
            startedAt = optLong("startedAt"),
            finishedAt = optLong("finishedAt"),
            rating = optInt("rating"),
            review = o.optString("review"),
            updatedAt = o.getLong("updatedAt"),
            deleted = o.optBoolean("deleted"),
        )
        val array = o.optJSONArray("logs") ?: JSONArray()
        val logs = buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONArray(i)
                put(item.getLong(0), BookLog(item.getInt(1), item.getLong(2)))
            }
        }
        return BookRecord(book, logs)
    }

    fun merge(a: BookRecord, b: BookRecord): BookRecord {
        val book = if (a.book.updatedAt >= b.book.updatedAt) a.book else b.book
        val logs = HashMap(a.logs)
        b.logs.forEach { (day, log) ->
            val mine = logs[day]
            if (mine == null || log.updatedAt > mine.updatedAt) logs[day] = log
        }
        return BookRecord(book, logs)
    }

    fun encode(g: Game): String = JSONObject().apply {
        put("id", g.id)
        put("steamAppId", g.steamAppId ?: JSONObject.NULL)
        put("title", g.title)
        put("developer", g.developer)
        put("year", g.year ?: JSONObject.NULL)
        put("platform", g.platform)
        put("cover", g.cover ?: JSONObject.NULL)
        put("status", g.status.name)
        put("startedAt", g.startedAt ?: JSONObject.NULL)
        put("completedAt", g.completedAt ?: JSONObject.NULL)
        put("rating", g.rating ?: JSONObject.NULL)
        put("review", g.review)
        put("updatedAt", g.updatedAt)
        put("deleted", g.deleted)
    }.toString()

    fun encode(w: WatchItem): String = JSONObject().apply {
        put("id", w.id)
        put("kind", w.kind.name)
        put("title", w.title)
        put("year", w.year ?: JSONObject.NULL)
        put("info", w.info)
        put("cover", w.cover ?: JSONObject.NULL)
        put("status", w.status.name)
        put("season", w.season)
        put("episode", w.episode)
        put("startedAt", w.startedAt ?: JSONObject.NULL)
        put("finishedAt", w.finishedAt ?: JSONObject.NULL)
        put("rating", w.rating ?: JSONObject.NULL)
        put("review", w.review)
        put("updatedAt", w.updatedAt)
        put("deleted", w.deleted)
    }.toString()

    fun decodeWatch(json: String): WatchItem {
        val o = JSONObject(json)
        fun optString(key: String) = if (o.isNull(key)) null else o.getString(key)
        fun optInt(key: String) = if (o.isNull(key)) null else o.getInt(key)
        fun optLong(key: String) = if (o.isNull(key)) null else o.getLong(key)
        return WatchItem(
            id = o.getString("id"),
            kind = WatchKind.entries.firstOrNull { it.name == o.optString("kind") } ?: WatchKind.Movie,
            title = o.getString("title"),
            year = optInt("year"),
            info = o.optString("info"),
            cover = optString("cover"),
            status = WatchStatus.entries.firstOrNull { it.name == o.optString("status") } ?: WatchStatus.Watched,
            season = o.optInt("season"),
            episode = o.optInt("episode"),
            startedAt = optLong("startedAt"),
            finishedAt = optLong("finishedAt"),
            rating = optInt("rating"),
            review = o.optString("review"),
            updatedAt = o.getLong("updatedAt"),
            deleted = o.optBoolean("deleted"),
        )
    }

    fun decodeGame(json: String): Game {
        val o = JSONObject(json)
        fun optString(key: String) = if (o.isNull(key)) null else o.getString(key)
        fun optInt(key: String) = if (o.isNull(key)) null else o.getInt(key)
        fun optLong(key: String) = if (o.isNull(key)) null else o.getLong(key)
        return Game(
            id = o.getString("id"),
            steamAppId = optLong("steamAppId"),
            title = o.getString("title"),
            developer = o.optString("developer"),
            year = optInt("year"),
            platform = o.optString("platform", "PC"),
            cover = optString("cover"),
            status = GameStatus.entries.firstOrNull { it.name == o.optString("status") } ?: GameStatus.Playing,
            startedAt = optLong("startedAt"),
            completedAt = optLong("completedAt"),
            rating = optInt("rating"),
            review = o.optString("review"),
            updatedAt = o.getLong("updatedAt"),
            deleted = o.optBoolean("deleted"),
        )
    }
}
