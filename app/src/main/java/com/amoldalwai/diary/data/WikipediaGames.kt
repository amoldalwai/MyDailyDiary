package com.amoldalwai.diary.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Game lookup on English Wikipedia (MediaWiki action API, no key). It covers console and delisted games Steam lacks;
 * cover art is the article's infobox image, which the API only exposes as a file name for non-free images.
 */
object WikipediaGames {
    const val USER_AGENT = "MyDailyDiary/1.1 (https://github.com/amoldalwai/MyDailyDiary)"

    // Descriptions such as "Racing video game series" or "British video game developer" are not games.
    private val notAGame = Regex(
        "series|franchise|developer|publisher|company|studio|character|soundtrack|film|television|comic|novel|engine|genre|console",
        RegexOption.IGNORE_CASE,
    )
    private val leadingYear = Regex("^(\\d{4})")
    private val disambiguation = Regex("\\s*\\([^)]*video game[^)]*\\)\\s*$", RegexOption.IGNORE_CASE)

    /** Exact search first; with fewer than three games found, retry with `word~` fuzzy matching on the longer words. */
    suspend fun search(query: String): List<GameResult> {
        val exact = searchPages("$query video game")
        if (exact.size >= 3) return exact
        val fuzzy = query.trim().split(Regex("\\s+")).joinToString(" ") { if (it.length >= 4) "$it~" else it }
        if (fuzzy == query.trim()) return exact
        return (exact + searchPages("$fuzzy video game")).distinctBy { it.title + it.year }.take(8)
    }

    private suspend fun searchPages(searchTerm: String): List<GameResult> = withContext(Dispatchers.IO) {
        val term = URLEncoder.encode(searchTerm, "UTF-8")
        val url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$term&gsrlimit=12" +
            "&prop=pageprops%7Cdescription&ppprop=page_image%7Cpage_image_free&format=json&formatversion=2"
        val pages = JSONObject(String(get(url))).optJSONObject("query")?.optJSONArray("pages") ?: return@withContext emptyList()
        List(pages.length()) { pages.getJSONObject(it) }
            .sortedBy { it.optInt("index") }
            .mapNotNull { page ->
                val description = page.optString("description")
                if (!description.contains("video game", ignoreCase = true) || notAGame.containsMatchIn(description)) return@mapNotNull null
                val props = page.optJSONObject("pageprops")
                val image = props?.optString("page_image").orEmpty().ifEmpty { props?.optString("page_image_free").orEmpty() }
                GameResult(
                    appId = 0,
                    title = page.optString("title").replace(disambiguation, ""),
                    year = leadingYear.find(description)?.value?.toInt(),
                    coverUrl = image.takeIf { it.isNotEmpty() }?.let(::fileUrl),
                    source = "Wikipedia",
                )
            }
            .take(8)
    }

    suspend fun downloadCover(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { get(url) }.getOrNull()
    }

    internal fun fileUrl(name: String) =
        "https://en.wikipedia.org/wiki/Special:FilePath/${URLEncoder.encode(name, "UTF-8").replace("+", "%20")}?width=500"

    internal fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            if (conn.responseCode !in 200..299) throw IOException("Wikipedia returned ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
