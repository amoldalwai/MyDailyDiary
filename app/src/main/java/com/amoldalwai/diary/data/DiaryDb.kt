package com.amoldalwai.diary.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.amoldalwai.diary.util.RichText

class DiaryDb private constructor(context: Context) : SQLiteOpenHelper(context, "diary.db", null, 8) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE entries (
                id TEXT PRIMARY KEY,
                date_epoch INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                mood TEXT,
                bg TEXT NOT NULL,
                font TEXT NOT NULL,
                font_scale REAL NOT NULL,
                images TEXT NOT NULL,
                word_count INTEGER NOT NULL,
                image_count INTEGER NOT NULL,
                month_day INTEGER NOT NULL,
                lines TEXT NOT NULL DEFAULT 'ruled',
                line_color TEXT NOT NULL DEFAULT 'auto',
                margin INTEGER NOT NULL DEFAULT 1,
                spans TEXT NOT NULL DEFAULT '[]',
                links TEXT NOT NULL DEFAULT '[]'
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_entries_date ON entries(date_epoch DESC, created_at DESC)")
        db.execSQL("CREATE INDEX idx_entries_month_day ON entries(month_day)")
        db.execSQL("CREATE INDEX idx_entries_mood ON entries(mood)")
        db.execSQL(
            """
            CREATE TABLE tags (
                entry_id TEXT NOT NULL,
                tag TEXT NOT NULL,
                PRIMARY KEY (entry_id, tag)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_tags_tag ON tags(tag)")
        createTombstones(db)
        createHabitsAndBooks(db)
        createGames(db)
        addReviewColumns(db)
        createWatch(db)
    }

    private fun createWatch(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE watch_items (
                id TEXT PRIMARY KEY,
                kind TEXT NOT NULL,
                title TEXT NOT NULL,
                year INTEGER,
                info TEXT NOT NULL,
                cover TEXT,
                status TEXT NOT NULL,
                season INTEGER NOT NULL,
                episode INTEGER NOT NULL,
                started_at INTEGER,
                finished_at INTEGER,
                rating INTEGER,
                review TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
    }

    /** Ratings (1-10) and short reviews for books and games; safe to run on any schema version. */
    private fun addReviewColumns(db: SQLiteDatabase) {
        for (table in listOf("books", "games")) {
            val existing = buildSet {
                db.rawQuery("PRAGMA table_info($table)", null).use { while (it.moveToNext()) add(it.getString(1)) }
            }
            if ("rating" !in existing) db.execSQL("ALTER TABLE $table ADD COLUMN rating INTEGER")
            if ("review" !in existing) db.execSQL("ALTER TABLE $table ADD COLUMN review TEXT NOT NULL DEFAULT ''")
        }
    }

    private fun createGames(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE games (
                id TEXT PRIMARY KEY,
                steam_app_id INTEGER,
                title TEXT NOT NULL,
                developer TEXT NOT NULL,
                year INTEGER,
                platform TEXT NOT NULL,
                cover TEXT,
                status TEXT NOT NULL,
                started_at INTEGER,
                completed_at INTEGER,
                updated_at INTEGER NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
    }

    private fun createHabitsAndBooks(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE habits (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                emoji TEXT NOT NULL,
                position INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE habit_checks (
                habit_id TEXT NOT NULL,
                date_epoch INTEGER NOT NULL,
                done INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (habit_id, date_epoch)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_checks_date ON habit_checks(date_epoch)")
        db.execSQL(
            """
            CREATE TABLE books (
                id TEXT PRIMARY KEY,
                ol_key TEXT,
                title TEXT NOT NULL,
                authors TEXT NOT NULL,
                year INTEGER,
                cover TEXT,
                total_pages INTEGER,
                current_page INTEGER NOT NULL,
                status TEXT NOT NULL,
                started_at INTEGER,
                finished_at INTEGER,
                updated_at INTEGER NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE book_logs (
                book_id TEXT NOT NULL,
                date_epoch INTEGER NOT NULL,
                page INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (book_id, date_epoch)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_logs_date ON book_logs(date_epoch)")
    }

    private fun createTombstones(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE deleted_entries (id TEXT PRIMARY KEY, deleted_at INTEGER NOT NULL, images TEXT NOT NULL)",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE entries ADD COLUMN lines TEXT NOT NULL DEFAULT 'ruled'")
            db.execSQL("ALTER TABLE entries ADD COLUMN line_color TEXT NOT NULL DEFAULT 'auto'")
            db.execSQL("ALTER TABLE entries ADD COLUMN margin INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE entries ADD COLUMN spans TEXT NOT NULL DEFAULT '[]'")
            convertMarkerFormatting(db)
        }
        if (oldVersion < 3) createTombstones(db)
        if (oldVersion < 4) createHabitsAndBooks(db)
        if (oldVersion < 5) createGames(db)
        if (oldVersion < 6) addReviewColumns(db)
        if (oldVersion < 7) createWatch(db)
        if (oldVersion < 8) db.execSQL("ALTER TABLE entries ADD COLUMN links TEXT NOT NULL DEFAULT '[]'")
    }

    /** Version 1 stored bold/italic as `**`/`*` markers inside the body. */
    private fun convertMarkerFormatting(db: SQLiteDatabase) {
        val updates = mutableListOf<Triple<String, String, String>>()
        db.rawQuery("SELECT id, body FROM entries WHERE body LIKE '%*%'", null).use { c ->
            while (c.moveToNext()) {
                val (plain, spans) = RichText.parseLegacy(c.getString(1))
                if (spans.isNotEmpty()) updates += Triple(c.getString(0), plain, RichText.spansToJson(spans))
            }
        }
        updates.forEach { (id, plain, json) ->
            db.execSQL("UPDATE entries SET body = ?, spans = ? WHERE id = ?", arrayOf(plain, json, id))
        }
    }

    companion object {
        @Volatile
        private var instance: DiaryDb? = null

        fun get(context: Context): DiaryDb =
            instance ?: synchronized(this) {
                instance ?: DiaryDb(context.applicationContext).also { instance = it }
            }
    }
}
