package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

class GamesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase
    private val imagesDir = File(appContext.filesDir, "diary/images").apply { mkdirs() }

    fun imageFile(name: String) = File(imagesDir, name)

    suspend fun games(): List<Game> = withContext(Dispatchers.IO) { query("WHERE deleted = 0 ORDER BY updated_at DESC") }

    suspend fun save(game: Game) = withContext(Dispatchers.IO) { upsert(game) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val game = query("WHERE id = ?", id).firstOrNull() ?: return@withContext
        upsert(game.copy(deleted = true, updatedAt = System.currentTimeMillis()))
        game.cover?.let { imageFile(it).delete() }
    }

    /** Stores a downloaded cover, shrunk to about 500 px wide, and returns its file name. */
    suspend fun saveCover(bytes: ByteArray): String? = withContext(Dispatchers.IO) { CoverStore.save(imagesDir, "game_", bytes) }

    // ---- Sync ----

    suspend fun allGames(): List<Game> = withContext(Dispatchers.IO) { query("") }

    suspend fun apply(game: Game) = withContext(Dispatchers.IO) { upsert(game) }

    // ---- Internals ----

    private fun query(tail: String, vararg args: String): List<Game> = buildList {
        db.rawQuery(
            "SELECT id, steam_app_id, title, developer, year, platform, cover, status, started_at, completed_at, updated_at, deleted, rating, review FROM games $tail",
            args.takeIf { it.isNotEmpty() }?.let { arrayOf(*it) },
        ).use { while (it.moveToNext()) add(it.toGame()) }
    }

    private fun Cursor.toGame() = Game(
        id = getString(0),
        steamAppId = if (isNull(1)) null else getLong(1),
        title = getString(2),
        developer = getString(3),
        year = if (isNull(4)) null else getInt(4),
        platform = getString(5),
        cover = if (isNull(6)) null else getString(6),
        status = GameStatus.entries.firstOrNull { it.name == getString(7) } ?: GameStatus.Playing,
        startedAt = if (isNull(8)) null else getLong(8),
        completedAt = if (isNull(9)) null else getLong(9),
        updatedAt = getLong(10),
        deleted = getInt(11) == 1,
        rating = if (isNull(12)) null else getInt(12),
        review = getString(13).orEmpty(),
    )

    private fun upsert(g: Game) {
        db.insertWithOnConflict(
            "games",
            null,
            ContentValues().apply {
                put("id", g.id)
                put("steam_app_id", g.steamAppId)
                put("title", g.title)
                put("developer", g.developer)
                put("year", g.year)
                put("platform", g.platform)
                put("cover", g.cover)
                put("status", g.status.name)
                put("started_at", g.startedAt)
                put("completed_at", g.completedAt)
                put("updated_at", g.updatedAt)
                put("deleted", if (g.deleted) 1 else 0)
                put("rating", g.rating)
                put("review", g.review)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}
