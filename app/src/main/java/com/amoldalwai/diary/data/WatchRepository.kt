package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WatchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase
    private val imagesDir = File(appContext.filesDir, "diary/images").apply { mkdirs() }

    fun imageFile(name: String) = File(imagesDir, name)

    suspend fun items(): List<WatchItem> = withContext(Dispatchers.IO) { query("WHERE deleted = 0 ORDER BY updated_at DESC") }

    suspend fun save(item: WatchItem) = withContext(Dispatchers.IO) { upsert(item) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val item = query("WHERE id = ?", id).firstOrNull() ?: return@withContext
        upsert(item.copy(deleted = true, updatedAt = System.currentTimeMillis()))
        item.cover?.let { imageFile(it).delete() }
    }

    suspend fun saveCover(bytes: ByteArray): String? = withContext(Dispatchers.IO) { CoverStore.save(imagesDir, "watch_", bytes) }

    // ---- Sync ----

    suspend fun allItems(): List<WatchItem> = withContext(Dispatchers.IO) { query("") }

    suspend fun apply(item: WatchItem) = withContext(Dispatchers.IO) { upsert(item) }

    // ---- Internals ----

    private fun query(tail: String, vararg args: String): List<WatchItem> = buildList {
        db.rawQuery(
            "SELECT id, kind, title, year, info, cover, status, season, episode, started_at, finished_at, rating, review, updated_at, deleted, finished_year FROM watch_items $tail",
            args.takeIf { it.isNotEmpty() }?.let { arrayOf(*it) },
        ).use { while (it.moveToNext()) add(it.toItem()) }
    }

    private fun Cursor.toItem() = WatchItem(
        id = getString(0),
        kind = WatchKind.entries.firstOrNull { it.name == getString(1) } ?: WatchKind.Movie,
        title = getString(2),
        year = if (isNull(3)) null else getInt(3),
        info = getString(4),
        cover = if (isNull(5)) null else getString(5),
        status = WatchStatus.entries.firstOrNull { it.name == getString(6) } ?: WatchStatus.Watched,
        season = getInt(7),
        episode = getInt(8),
        startedAt = if (isNull(9)) null else getLong(9),
        finishedAt = if (isNull(10)) null else getLong(10),
        rating = if (isNull(11)) null else getInt(11),
        review = getString(12).orEmpty(),
        updatedAt = getLong(13),
        deleted = getInt(14) == 1,
        finishedYear = if (isNull(15)) null else getInt(15),
    )

    private fun upsert(w: WatchItem) {
        db.insertWithOnConflict(
            "watch_items",
            null,
            ContentValues().apply {
                put("id", w.id)
                put("kind", w.kind.name)
                put("title", w.title)
                put("year", w.year)
                put("info", w.info)
                put("cover", w.cover)
                put("status", w.status.name)
                put("season", w.season)
                put("episode", w.episode)
                put("started_at", w.startedAt)
                put("finished_at", w.finishedAt)
                put("rating", w.rating)
                put("review", w.review)
                put("updated_at", w.updatedAt)
                put("deleted", if (w.deleted) 1 else 0)
                put("finished_year", w.finishedYear)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}
