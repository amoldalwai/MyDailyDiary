package com.amoldalwai.diary.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class BookResult(
    val key: String,
    val title: String,
    val authors: String,
    val year: Int?,
    val coverId: Long?,
    val pages: Int?,
)

/** Open Library search and covers (https://openlibrary.org/dev/docs/api/search); no account or key needed. */
object OpenLibrary {
    private const val USER_AGENT = "LifeLoom/1.2 (https://github.com/amoldalwai/MyDailyDiary)"

    fun coverUrl(coverId: Long, size: String = "M") = "https://covers.openlibrary.org/b/id/$coverId-$size.jpg"

    suspend fun search(query: String): List<BookResult> = withContext(Dispatchers.IO) {
        val fields = "key,title,author_name,first_publish_year,cover_i,number_of_pages_median"
        val url = "https://openlibrary.org/search.json?q=${URLEncoder.encode(query, "UTF-8")}&limit=15&fields=$fields"
        val json = JSONObject(String(get(url)))
        val docs = json.optJSONArray("docs") ?: return@withContext emptyList()
        List(docs.length()) { i ->
            val d = docs.getJSONObject(i)
            val authors = d.optJSONArray("author_name")
            BookResult(
                key = d.optString("key"),
                title = d.optString("title"),
                authors = authors?.let { a -> List(minOf(a.length(), 3)) { a.getString(it) }.joinToString(", ") }.orEmpty(),
                year = if (d.has("first_publish_year")) d.optInt("first_publish_year") else null,
                coverId = if (d.has("cover_i")) d.optLong("cover_i") else null,
                pages = if (d.has("number_of_pages_median")) d.optInt("number_of_pages_median").takeIf { it > 0 } else null,
            )
        }.filter { it.title.isNotBlank() }
    }

    suspend fun downloadCover(coverId: Long): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { get(coverUrl(coverId, "L")) }.getOrNull()
    }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            if (conn.responseCode !in 200..299) throw IOException("Open Library returned ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
