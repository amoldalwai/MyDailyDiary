package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/** A book with its reading log; the unit that is synced to Drive as one file. */
data class BookRecord(val book: Book, val logs: Map<Long, BookLog>) {
    val maxUpdated: Long get() = max(book.updatedAt, logs.values.maxOfOrNull { it.updatedAt } ?: 0L)
}

class BooksRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase
    private val imagesDir = File(appContext.filesDir, "diary/images").apply { mkdirs() }

    fun imageFile(name: String) = File(imagesDir, name)

    suspend fun books(): List<Book> = withContext(Dispatchers.IO) {
        queryBooks("WHERE deleted = 0 ORDER BY updated_at DESC")
    }

    suspend fun save(book: Book) = withContext(Dispatchers.IO) { upsertBook(book) }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val book = queryBooks("WHERE id = '${id.replace("'", "''")}'").firstOrNull() ?: return@withContext
        upsertBook(book.copy(deleted = true, updatedAt = System.currentTimeMillis()))
        book.cover?.let { imageFile(it).delete() }
    }

    /** Page each book had reached on [epochDay], keyed by book id. */
    suspend fun logsOn(epochDay: Long): Map<String, Int> = withContext(Dispatchers.IO) {
        buildMap {
            db.rawQuery("SELECT book_id, page FROM book_logs WHERE date_epoch = ?", arrayOf(epochDay.toString())).use {
                while (it.moveToNext()) put(it.getString(0), it.getInt(1))
            }
        }
    }

    /** Records the page reached on a day and moves a book on the want list to Reading; finishing is left to the caller, which asks for a rating. */
    suspend fun logPage(bookId: String, epochDay: Long, page: Int): Book? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        upsertLog(bookId, epochDay, BookLog(page, now))
        var latest = page
        db.rawQuery("SELECT page FROM book_logs WHERE book_id = ? ORDER BY date_epoch DESC LIMIT 1", arrayOf(bookId)).use {
            if (it.moveToFirst()) latest = it.getInt(0)
        }
        val book = queryBooks("WHERE id = '${bookId.replace("'", "''")}'").firstOrNull() ?: return@withContext null
        var status = book.status
        var started = book.startedAt
        if (status == BookStatus.Want) {
            status = BookStatus.Reading
            started = started ?: now
        }
        val updated = book.copy(currentPage = latest, status = status, startedAt = started, updatedAt = now)
        upsertBook(updated)
        updated
    }

    /** Stores a downloaded cover as a small poster and returns its file name. */
    suspend fun saveCover(bytes: ByteArray): String? = withContext(Dispatchers.IO) { CoverStore.save(imagesDir, "book_", bytes) }

    // ---- Sync ----

    suspend fun allRecords(): List<BookRecord> = withContext(Dispatchers.IO) {
        val logs = HashMap<String, MutableMap<Long, BookLog>>()
        db.rawQuery("SELECT book_id, date_epoch, page, updated_at FROM book_logs", null).use {
            while (it.moveToNext()) {
                logs.getOrPut(it.getString(0)) { mutableMapOf() }[it.getLong(1)] = BookLog(it.getInt(2), it.getLong(3))
            }
        }
        queryBooks("").map { BookRecord(it, logs[it.id].orEmpty()) }
    }

    suspend fun applyRecord(record: BookRecord) = withContext(Dispatchers.IO) {
        db.beginTransaction()
        try {
            upsertBook(record.book)
            record.logs.forEach { (day, log) -> upsertLog(record.book.id, day, log) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---- Internals ----

    private fun queryBooks(tail: String): List<Book> = buildList {
        db.rawQuery(
            "SELECT id, ol_key, title, authors, year, cover, total_pages, current_page, status, started_at, finished_at, updated_at, deleted, rating, review, finished_year FROM books $tail",
            null,
        ).use { while (it.moveToNext()) add(it.toBook()) }
    }

    private fun Cursor.toBook() = Book(
        id = getString(0),
        openLibraryKey = if (isNull(1)) null else getString(1),
        title = getString(2),
        authors = getString(3),
        year = if (isNull(4)) null else getInt(4),
        cover = if (isNull(5)) null else getString(5),
        totalPages = if (isNull(6)) null else getInt(6),
        currentPage = getInt(7),
        status = BookStatus.entries.firstOrNull { it.name == getString(8) } ?: BookStatus.Reading,
        startedAt = if (isNull(9)) null else getLong(9),
        finishedAt = if (isNull(10)) null else getLong(10),
        updatedAt = getLong(11),
        deleted = getInt(12) == 1,
        rating = if (isNull(13)) null else getInt(13),
        review = getString(14).orEmpty(),
        finishedYear = if (isNull(15)) null else getInt(15),
    )

    private fun upsertBook(b: Book) {
        db.insertWithOnConflict(
            "books",
            null,
            ContentValues().apply {
                put("id", b.id)
                put("ol_key", b.openLibraryKey)
                put("title", b.title)
                put("authors", b.authors)
                put("year", b.year)
                put("cover", b.cover)
                put("total_pages", b.totalPages)
                put("current_page", b.currentPage)
                put("status", b.status.name)
                put("started_at", b.startedAt)
                put("finished_at", b.finishedAt)
                put("updated_at", b.updatedAt)
                put("deleted", if (b.deleted) 1 else 0)
                put("rating", b.rating)
                put("review", b.review)
                put("finished_year", b.finishedYear)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun upsertLog(bookId: String, day: Long, log: BookLog) {
        db.insertWithOnConflict(
            "book_logs",
            null,
            ContentValues().apply {
                put("book_id", bookId)
                put("date_epoch", day)
                put("page", log.page)
                put("updated_at", log.updatedAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }
}
