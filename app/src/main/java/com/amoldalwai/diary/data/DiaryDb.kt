package com.amoldalwai.diary.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.amoldalwai.diary.util.RichText

class DiaryDb private constructor(context: Context) : SQLiteOpenHelper(context, "diary.db", null, 3) {

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
                spans TEXT NOT NULL DEFAULT '[]'
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
