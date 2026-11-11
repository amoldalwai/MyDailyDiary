package com.amoldalwai.diary.data

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

data class Quote(val text: String, val author: String)

/** Motivational quotes shipped with the app (content/quotes.json); one per day, the same all day. */
object Quotes {
    @Volatile
    private var cache: List<Quote>? = null

    private fun load(context: Context): List<Quote> = cache ?: synchronized(this) {
        cache ?: runCatching {
            val json = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Quote(o.getString("quote"), o.optString("author"))
            }
        }.getOrDefault(emptyList()).also { cache = it }
    }

    fun forDay(context: Context, day: LocalDate): Quote? {
        val all = load(context)
        if (all.isEmpty()) return null
        return all[Math.floorMod(day.toEpochDay(), all.size.toLong()).toInt()]
    }
}
