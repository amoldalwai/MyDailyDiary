package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.amoldalwai.diary.util.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** Keyset pagination position: the last entry already shown. */
data class PageCursor(val dateEpochDay: Long, val createdAt: Long)

data class DayMarker(val count: Int, val mood: Mood?)

data class HomeStats(val total: Int = 0, val thisMonth: Int = 0, val streak: Int = 0)

data class InsightStats(
    val total: Int,
    val words: Int,
    val current: Int,
    val longest: Int,
    val photos: Int,
    val moods: List<Pair<Mood, Int>>,
    val weekly: List<Int>,
    val tags: List<Pair<String, Int>>,
)

data class PhotoRef(val entryId: String, val name: String, val dateEpochDay: Long)

data class PhotoPage(val photos: List<PhotoRef>, val next: PageCursor?, val hasMore: Boolean)

data class StorageUsage(val dataBytes: Long, val photoBytes: Long, val photoCount: Int, val entryCount: Int) {
    val totalBytes: Long get() = dataBytes + photoBytes
}

/**
 * Entries live in SQLite. Lists only read a short body preview and use indexed keyset pagination;
 * counts and charts are answered by SQL aggregates, so the full text is read only when one entry is opened.
 * Photos stay as JPEG files in `diary/images`.
 */
class DiaryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase
    private val root = File(appContext.filesDir, "diary")
    private val legacyDir = File(root, "entries")
    private val imagesDir = File(root, "images").apply { mkdirs() }

    fun imageFile(name: String) = File(imagesDir, name)

    private val profileDir = File(appContext.filesDir, "profile").apply { mkdirs() }

    fun profileFile(name: String) = File(profileDir, name)

    suspend fun deleteProfileFile(name: String) = withContext(Dispatchers.IO) { profileFile(name).delete() }

    /** Saves a picked photo as a square 640 px JPEG for the profile avatar and returns its file name. */
    suspend fun importProfilePhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val shortest = minOf(info.size.width, info.size.height)
                if (shortest > 640) {
                    val ratio = 640f / shortest
                    decoder.setTargetSize((info.size.width * ratio).roundToInt(), (info.size.height * ratio).roundToInt())
                }
            }
            val side = minOf(bitmap.width, bitmap.height)
            val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
            val name = "profile_${UUID.randomUUID()}.jpg"
            profileFile(name).outputStream().use { square.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        }.getOrNull()
    }

    // ---- Reads ----

    suspend fun getEntry(id: String): DiaryEntry? = withContext(Dispatchers.IO) {
        listEntries(full = true, where = "e.id = ?", args = listOf(id), cursor = null, limit = 1).firstOrNull()
    }

    suspend fun page(cursor: PageCursor?, limit: Int): List<DiaryEntry> = withContext(Dispatchers.IO) {
        listEntries(full = false, where = "", args = emptyList(), cursor = cursor, limit = limit)
    }

    /** The next entry in time ([newer] = true) or the previous one, or null at either end. */
    suspend fun neighbor(entry: DiaryEntry, newer: Boolean): DiaryEntry? = withContext(Dispatchers.IO) {
        val op = if (newer) ">" else "<"
        listEntries(
            full = true,
            where = "(e.date_epoch $op ? OR (e.date_epoch = ? AND e.created_at $op ?))",
            args = listOf(entry.dateEpochDay.toString(), entry.dateEpochDay.toString(), entry.createdAt.toString()),
            cursor = null,
            limit = 1,
            ascending = newer,
        ).firstOrNull()
    }

    suspend fun entriesOn(epochDay: Long): List<DiaryEntry> = withContext(Dispatchers.IO) {
        listEntries(full = false, where = "e.date_epoch = ?", args = listOf(epochDay.toString()), cursor = null, limit = 0)
    }

    suspend fun onThisDay(today: LocalDate): List<DiaryEntry> = withContext(Dispatchers.IO) {
        listEntries(
            full = false,
            where = "e.month_day = ? AND e.date_epoch < ?",
            args = listOf((today.monthValue * 100 + today.dayOfMonth).toString(), LocalDate.of(today.year, 1, 1).toEpochDay().toString()),
            cursor = null,
            limit = 5,
        )
    }

    /** Full entries (with complete text) for exports; [from]/[to] are inclusive epoch days. */
    suspend fun fullEntries(from: Long?, to: Long?): List<DiaryEntry> = withContext(Dispatchers.IO) {
        val where = buildList {
            if (from != null) add("e.date_epoch >= $from")
            if (to != null) add("e.date_epoch <= $to")
        }.joinToString(" AND ")
        listEntries(full = true, where = where, args = emptyList(), cursor = null, limit = 0)
    }

    suspend fun search(tokens: List<String>, mood: Mood?, tag: String?, cursor: PageCursor?, limit: Int): List<DiaryEntry> =
        withContext(Dispatchers.IO) {
            val (where, args) = searchWhere(tokens, mood, tag)
            listEntries(full = false, where = where, args = args, cursor = cursor, limit = limit)
        }

    suspend fun searchCount(tokens: List<String>, mood: Mood?, tag: String?): Int = withContext(Dispatchers.IO) {
        val (where, args) = searchWhere(tokens, mood, tag)
        scalar("SELECT COUNT(*) FROM entries e" + if (where.isEmpty()) "" else " WHERE $where", args)
    }

    suspend fun homeStats(today: LocalDate): HomeStats = withContext(Dispatchers.IO) {
        val first = today.withDayOfMonth(1).toEpochDay()
        val last = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()
        HomeStats(
            total = scalar("SELECT COUNT(*) FROM entries"),
            thisMonth = scalar("SELECT COUNT(*) FROM entries WHERE date_epoch BETWEEN $first AND $last"),
            streak = currentStreak(today.toEpochDay()),
        )
    }

    suspend fun daysWithEntries(from: Long, to: Long): Set<Long> = withContext(Dispatchers.IO) {
        buildSet {
            db.rawQuery("SELECT DISTINCT date_epoch FROM entries WHERE date_epoch BETWEEN $from AND $to", null).use {
                while (it.moveToNext()) add(it.getLong(0))
            }
        }
    }

    suspend fun monthMarkers(from: Long, to: Long): Map<Long, DayMarker> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT e.date_epoch, COUNT(*),
                (SELECT m.mood FROM entries m
                 WHERE m.date_epoch = e.date_epoch AND m.mood IS NOT NULL
                 ORDER BY m.created_at DESC LIMIT 1)
            FROM entries e
            WHERE e.date_epoch BETWEEN $from AND $to
            GROUP BY e.date_epoch
        """.trimIndent()
        buildMap {
            db.rawQuery(sql, null).use {
                while (it.moveToNext()) put(it.getLong(0), DayMarker(it.getInt(1), moodOf(it.getString(2))))
            }
        }
    }

    suspend fun tagCounts(limit: Int): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
        buildList {
            db.rawQuery("SELECT tag, COUNT(*) AS c FROM tags GROUP BY tag ORDER BY c DESC, tag LIMIT $limit", null).use {
                while (it.moveToNext()) add(it.getString(0) to it.getInt(1))
            }
        }
    }

    suspend fun insightStats(today: LocalDate): InsightStats = withContext(Dispatchers.IO) {
        val t = today.toEpochDay()
        val weekly = IntArray(8)
        db.rawQuery(
            "SELECT ($t - date_epoch) / 7 AS wk, COUNT(*) FROM entries WHERE date_epoch BETWEEN ${t - 55} AND $t GROUP BY wk",
            null,
        ).use {
            while (it.moveToNext()) {
                val wk = it.getInt(0)
                if (wk in 0..7) weekly[7 - wk] = it.getInt(1)
            }
        }
        val moods = buildList {
            db.rawQuery("SELECT mood, COUNT(*) AS c FROM entries WHERE mood IS NOT NULL GROUP BY mood ORDER BY c DESC", null).use {
                while (it.moveToNext()) moodOf(it.getString(0))?.let { m -> add(m to it.getInt(1)) }
            }
        }
        var longest = 0
        var run = 0
        var prev = Long.MIN_VALUE
        db.rawQuery("SELECT DISTINCT date_epoch FROM entries ORDER BY date_epoch", null).use {
            while (it.moveToNext()) {
                val d = it.getLong(0)
                run = if (d == prev + 1) run + 1 else 1
                longest = max(longest, run)
                prev = d
            }
        }
        InsightStats(
            total = scalar("SELECT COUNT(*) FROM entries"),
            words = scalar("SELECT COALESCE(SUM(word_count), 0) FROM entries"),
            current = currentStreak(t),
            longest = longest,
            photos = scalar("SELECT COALESCE(SUM(image_count), 0) FROM entries"),
            moods = moods,
            weekly = weekly.toList(),
            tags = tagCountsBlocking(10),
        )
    }

    /** Photos of the next [limit] entries that have any, newest first, for the stamp album. */
    suspend fun photoPage(cursor: PageCursor?, limit: Int): PhotoPage = withContext(Dispatchers.IO) {
        val where = if (cursor == null) {
            "image_count > 0"
        } else {
            "image_count > 0 AND (date_epoch < ${cursor.dateEpochDay} OR (date_epoch = ${cursor.dateEpochDay} AND created_at < ${cursor.createdAt}))"
        }
        val photos = mutableListOf<PhotoRef>()
        var next: PageCursor? = null
        var rows = 0
        db.rawQuery(
            "SELECT id, date_epoch, created_at, images FROM entries WHERE $where ORDER BY date_epoch DESC, created_at DESC LIMIT $limit",
            null,
        ).use {
            while (it.moveToNext()) {
                rows++
                next = PageCursor(it.getLong(1), it.getLong(2))
                parseList(it.getString(3)).forEach { name -> photos += PhotoRef(it.getString(0), name, it.getLong(1)) }
            }
        }
        PhotoPage(photos, next, hasMore = rows >= limit)
    }

    suspend fun photoTotal(): Int = withContext(Dispatchers.IO) {
        scalar("SELECT COALESCE(SUM(image_count), 0) FROM entries")
    }

    /** Entry count, photos that earn points (capped per entry) and the longest run of consecutive days, for the progress board. */
    suspend fun progressBasics(): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        var longest = 0
        var run = 0
        var prev = Long.MIN_VALUE
        db.rawQuery("SELECT DISTINCT date_epoch FROM entries ORDER BY date_epoch", null).use {
            while (it.moveToNext()) {
                val d = it.getLong(0)
                run = if (d == prev + 1) run + 1 else 1
                longest = max(longest, run)
                prev = d
            }
        }
        Triple(
            scalar("SELECT COUNT(*) FROM entries"),
            scalar("SELECT COALESCE(SUM(MIN(image_count, ${Progress.PHOTOS_PER_ENTRY})), 0) FROM entries"),
            longest,
        )
    }

    suspend fun storageUsage(): StorageUsage = withContext(Dispatchers.IO) {
        val dbFile = appContext.getDatabasePath("diary.db")
        val dataBytes = listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).filter { it.exists() }.sumOf { it.length() }
        val photos = imagesDir.listFiles().orEmpty()
        StorageUsage(
            dataBytes = dataBytes,
            photoBytes = photos.sumOf { it.length() },
            photoCount = photos.size,
            entryCount = scalar("SELECT COUNT(*) FROM entries"),
        )
    }

    /** Blocking, used by the reminder receiver. */
    fun hasEntryOn(epochDay: Long): Boolean {
        migrateLegacyBlocking()
        return scalar("SELECT COUNT(*) FROM entries WHERE date_epoch = $epochDay") > 0
    }

    // ---- Writes ----

    suspend fun save(entry: DiaryEntry) = withContext(Dispatchers.IO) { upsert(entry) }

    suspend fun delete(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            db.delete("tags", "entry_id = ?", arrayOf(entry.id))
            db.delete("entries", "id = ?", arrayOf(entry.id))
            db.insertWithOnConflict(
                "deleted_entries",
                null,
                ContentValues().apply {
                    put("id", entry.id)
                    put("deleted_at", System.currentTimeMillis())
                    put("images", JSONArray(entry.images).toString())
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        entry.images.forEach { imageFile(it).delete() }
    }

    // ---- Sync support ----

    data class Tombstone(val deletedAt: Long, val images: List<String>)

    suspend fun syncIndex(): Map<String, Long> = withContext(Dispatchers.IO) {
        buildMap {
            db.rawQuery("SELECT id, updated_at FROM entries", null).use {
                while (it.moveToNext()) put(it.getString(0), it.getLong(1))
            }
        }
    }

    suspend fun tombstones(): Map<String, Tombstone> = withContext(Dispatchers.IO) {
        buildMap {
            db.rawQuery("SELECT id, deleted_at, images FROM deleted_entries", null).use {
                while (it.moveToNext()) put(it.getString(0), Tombstone(it.getLong(1), parseList(it.getString(2))))
            }
        }
    }

    suspend fun clearTombstone(id: String) = withContext(Dispatchers.IO) {
        db.delete("deleted_entries", "id = ?", arrayOf(id))
    }

    /** Stores an entry received from the cloud as-is, keeping its original timestamps. */
    suspend fun applyRemote(entry: DiaryEntry) = withContext(Dispatchers.IO) {
        upsert(entry)
        db.delete("deleted_entries", "id = ?", arrayOf(entry.id))
    }

    /** Removes an entry that was deleted on another device. */
    suspend fun applyRemoteDelete(id: String) = withContext(Dispatchers.IO) {
        var images = emptyList<String>()
        db.rawQuery("SELECT images FROM entries WHERE id = ?", arrayOf(id)).use {
            if (it.moveToFirst()) images = parseList(it.getString(0))
        }
        db.beginTransaction()
        try {
            db.delete("tags", "entry_id = ?", arrayOf(id))
            db.delete("entries", "id = ?", arrayOf(id))
            db.delete("deleted_entries", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        images.forEach { imageFile(it).delete() }
    }

    suspend fun deleteImages(names: Collection<String>) = withContext(Dispatchers.IO) {
        names.forEach { imageFile(it).delete() }
    }

    suspend fun saveBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val name = "${UUID.randomUUID()}.jpg"
        imageFile(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }

    /** Copies a picked photo into app storage, downscaled and EXIF-rotated. Returns the stored file name. */
    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_IMAGE_SIDE) {
                    val ratio = MAX_IMAGE_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * ratio).roundToInt(),
                        (info.size.height * ratio).roundToInt(),
                    )
                }
            }
            val name = "${UUID.randomUUID()}.jpg"
            imageFile(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, it) }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    /** One-time import of entries saved by the earlier JSON-file version. */
    suspend fun migrateLegacy() = withContext(Dispatchers.IO) { migrateLegacyBlocking() }

    // ---- Internals ----

    private fun upsert(e: DiaryEntry) {
        val values = ContentValues().apply {
            put("id", e.id)
            put("date_epoch", e.dateEpochDay)
            put("created_at", e.createdAt)
            put("updated_at", e.updatedAt)
            put("title", e.title)
            put("body", e.body)
            put("mood", e.mood?.name)
            put("bg", e.style.backgroundId)
            put("font", e.style.fontId)
            put("font_scale", e.style.fontScale)
            put("images", JSONArray(e.images).toString())
            put("word_count", e.body.trim().split(Regex("\\s+")).count { it.isNotEmpty() })
            put("image_count", e.images.size)
            put("month_day", e.date.monthValue * 100 + e.date.dayOfMonth)
            put("lines", e.style.lines)
            put("line_color", e.style.lineColorId)
            put("margin", if (e.style.margin) 1 else 0)
            put("spans", RichText.spansToJson(e.spans))
            put("links", JSONArray(e.links).toString())
        }
        db.beginTransaction()
        try {
            db.insertWithOnConflict("entries", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.delete("tags", "entry_id = ?", arrayOf(e.id))
            e.tags.forEach { tag ->
                db.insert("tags", null, ContentValues().apply { put("entry_id", e.id); put("tag", tag) })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun listEntries(
        full: Boolean,
        where: String,
        args: List<String>,
        cursor: PageCursor?,
        limit: Int,
        ascending: Boolean = false,
    ): List<DiaryEntry> {
        val conditions = mutableListOf<String>()
        val params = args.toMutableList()
        if (where.isNotEmpty()) conditions += where
        if (cursor != null) {
            conditions += "(e.date_epoch < ? OR (e.date_epoch = ? AND e.created_at < ?))"
            params += cursor.dateEpochDay.toString()
            params += cursor.dateEpochDay.toString()
            params += cursor.createdAt.toString()
        }
        val body = if (full) "e.body" else "substr(e.body, 1, $PREVIEW_CHARS) AS body"
        val direction = if (ascending) "ASC" else "DESC"
        val sql = "SELECT e.id, e.date_epoch, e.created_at, e.updated_at, e.title, $body, e.mood, e.bg, e.font, e.font_scale, e.images, " +
            "e.lines, e.line_color, e.margin, e.spans, e.links " +
            "FROM entries e" +
            (if (conditions.isEmpty()) "" else " WHERE " + conditions.joinToString(" AND ")) +
            " ORDER BY e.date_epoch $direction, e.created_at $direction" +
            (if (limit > 0) " LIMIT $limit" else "")

        val rows = mutableListOf<DiaryEntry>()
        db.rawQuery(sql, params.toTypedArray()).use { c ->
            while (c.moveToNext()) {
                rows += DiaryEntry(
                    id = c.getString(0),
                    dateEpochDay = c.getLong(1),
                    createdAt = c.getLong(2),
                    updatedAt = c.getLong(3),
                    title = c.getString(4),
                    body = c.getString(5),
                    mood = moodOf(c.getString(6)),
                    style = PageStyle(
                        backgroundId = c.getString(7),
                        fontId = c.getString(8),
                        fontScale = c.getFloat(9),
                        lines = c.getString(11),
                        lineColorId = c.getString(12),
                        margin = c.getInt(13) == 1,
                    ),
                    images = parseList(c.getString(10)),
                    spans = RichText.spansFromJson(c.getString(14)),
                    links = parseList(c.getString(15)),
                )
            }
        }
        if (rows.isEmpty()) return rows
        val tags = tagsFor(rows.map { it.id })
        return rows.map { it.copy(tags = tags[it.id].orEmpty()) }
    }

    private fun tagsFor(ids: List<String>): Map<String, List<String>> {
        val result = HashMap<String, MutableList<String>>()
        ids.chunked(400).forEach { chunk ->
            val marks = chunk.joinToString(",") { "?" }
            db.rawQuery("SELECT entry_id, tag FROM tags WHERE entry_id IN ($marks) ORDER BY rowid", chunk.toTypedArray()).use {
                while (it.moveToNext()) result.getOrPut(it.getString(0)) { mutableListOf() }.add(it.getString(1))
            }
        }
        return result
    }

    private fun searchWhere(tokens: List<String>, mood: Mood?, tag: String?): Pair<String, List<String>> {
        val parts = mutableListOf<String>()
        val args = mutableListOf<String>()
        tokens.forEach { token ->
            val like = "%" + token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            parts += "(e.title LIKE ? ESCAPE '\\' OR e.body LIKE ? ESCAPE '\\' OR " +
                "EXISTS (SELECT 1 FROM tags t WHERE t.entry_id = e.id AND t.tag LIKE ? ESCAPE '\\'))"
            repeat(3) { args += like }
        }
        if (mood != null) {
            parts += "e.mood = ?"
            args += mood.name
        }
        if (tag != null) {
            parts += "EXISTS (SELECT 1 FROM tags t WHERE t.entry_id = e.id AND t.tag = ?)"
            args += tag
        }
        return parts.joinToString(" AND ") to args
    }

    private fun scalar(sql: String, args: List<String> = emptyList()): Int =
        db.rawQuery(sql, args.toTypedArray()).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun tagCountsBlocking(limit: Int): List<Pair<String, Int>> = buildList {
        db.rawQuery("SELECT tag, COUNT(*) AS c FROM tags GROUP BY tag ORDER BY c DESC, tag LIMIT $limit", null).use {
            while (it.moveToNext()) add(it.getString(0) to it.getInt(1))
        }
    }

    private fun currentStreak(today: Long): Int {
        db.rawQuery("SELECT DISTINCT date_epoch FROM entries WHERE date_epoch <= $today ORDER BY date_epoch DESC", null).use {
            var expected = Long.MIN_VALUE
            var count = 0
            while (it.moveToNext()) {
                val d = it.getLong(0)
                if (expected == Long.MIN_VALUE) expected = if (d == today) today else today - 1
                if (d != expected) break
                count++
                expected--
            }
            return count
        }
    }

    private fun moodOf(name: String?): Mood? = Mood.entries.firstOrNull { it.name == name }

    private fun parseList(json: String): List<String> {
        val array = JSONArray(json)
        return List(array.length()) { array.getString(it) }
    }

    @Synchronized
    private fun migrateLegacyBlocking() {
        val files = legacyDir.listFiles { f -> f.extension == "json" }.orEmpty()
        for (file in files) {
            val entry = runCatching { legacyEntry(JSONObject(file.readText())) }.getOrNull()
            if (entry != null) {
                upsert(entry)
                file.delete()
            }
        }
    }

    private fun legacyEntry(o: JSONObject): DiaryEntry {
        val style = o.optJSONObject("style")
        val (plainBody, spans) = RichText.parseLegacy(o.optString("body"))
        fun list(key: String): List<String> {
            val a = o.optJSONArray(key) ?: return emptyList()
            return List(a.length()) { a.getString(it) }
        }
        return DiaryEntry(
            id = o.getString("id"),
            dateEpochDay = o.getLong("dateEpochDay"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            title = o.optString("title"),
            body = plainBody,
            mood = moodOf(o.optString("mood")),
            style = PageStyle(
                backgroundId = style?.optString("backgroundId", "paper") ?: "paper",
                fontId = style?.optString("fontId", "serif") ?: "serif",
                fontScale = style?.optDouble("fontScale", 1.0)?.toFloat() ?: 1f,
            ),
            images = list("images"),
            tags = list("tags"),
            spans = spans,
        )
    }

    private companion object {
        const val MAX_IMAGE_SIDE = 1280
        const val PHOTO_QUALITY = 80
        const val PREVIEW_CHARS = 260
    }
}
