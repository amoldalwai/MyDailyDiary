package com.amoldalwai.diary.sync

import com.amoldalwai.diary.data.DiaryEntry
import com.amoldalwai.diary.data.Mood
import com.amoldalwai.diary.data.PageStyle
import com.amoldalwai.diary.util.RichText
import org.json.JSONArray
import org.json.JSONObject

/** JSON form of an entry as stored in the user's Drive app folder. */
object SyncCodec {
    fun encode(e: DiaryEntry): String = JSONObject().apply {
        put("id", e.id)
        put("dateEpochDay", e.dateEpochDay)
        put("createdAt", e.createdAt)
        put("updatedAt", e.updatedAt)
        put("title", e.title)
        put("body", e.body)
        put("mood", e.mood?.name ?: "")
        put("bg", e.style.backgroundId)
        put("font", e.style.fontId)
        put("fontScale", e.style.fontScale.toDouble())
        put("lines", e.style.lines)
        put("lineColor", e.style.lineColorId)
        put("margin", e.style.margin)
        put("images", JSONArray(e.images))
        put("tags", JSONArray(e.tags))
        put("spans", JSONArray(RichText.spansToJson(e.spans)))
    }.toString()

    fun decode(json: String): DiaryEntry {
        val o = JSONObject(json)
        fun strings(key: String): List<String> {
            val array = o.optJSONArray(key) ?: return emptyList()
            return List(array.length()) { array.getString(it) }
        }
        return DiaryEntry(
            id = o.getString("id"),
            dateEpochDay = o.getLong("dateEpochDay"),
            createdAt = o.getLong("createdAt"),
            updatedAt = o.getLong("updatedAt"),
            title = o.optString("title"),
            body = o.optString("body"),
            mood = Mood.entries.firstOrNull { it.name == o.optString("mood") },
            style = PageStyle(
                backgroundId = o.optString("bg", "paper"),
                fontId = o.optString("font", "serif"),
                fontScale = o.optDouble("fontScale", 1.0).toFloat(),
                lines = o.optString("lines", "ruled"),
                lineColorId = o.optString("lineColor", "auto"),
                margin = o.optBoolean("margin", true),
            ),
            images = strings("images"),
            tags = strings("tags"),
            spans = RichText.spansFromJson(o.optJSONArray("spans")?.toString()),
        )
    }
}
