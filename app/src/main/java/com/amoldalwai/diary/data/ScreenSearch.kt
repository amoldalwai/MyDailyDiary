package com.amoldalwai.diary.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import kotlin.math.max

data class WatchResult(
    val kind: WatchKind,
    val title: String,
    val year: Int?,
    val info: String,
    val coverUrl: String?,
    val thumbUrl: String?,
    val source: String,
)

/**
 * Movie and series lookup, no keys needed. Wikipedia is filtered with CirrusSearch's `hastemplate:` operator
 * (`Infobox film` for movies, `Infobox television` for series, including web series), and TVMaze adds real posters for series.
 */
object ScreenSearch {
    private val yearPattern = Regex("\\b(19|20)\\d{2}\\b")
    private val trailingBrackets = Regex("\\s*\\([^)]*\\)\\s*$")
    private val directedBy = Regex("film by (.+)$", RegexOption.IGNORE_CASE)
    private val tvWords = Regex("series|sitcom|soap opera|telenovela|drama|miniseries|anime|\\bshow\\b|serial|web", RegexOption.IGNORE_CASE)
    private val notTv = Regex("wrestling|talk show|news|podcast|franchise", RegexOption.IGNORE_CASE)

    suspend fun search(query: String): List<WatchResult> = coroutineScope {
        val films = async { runCatching { wikipedia(query, "Infobox film", WatchKind.Movie) } }
        val maze = async { runCatching { tvMaze(query) } }
        val wikiSeries = async { runCatching { wikipedia(query, "Infobox television", WatchKind.Series) } }
        val f = films.await()
        val m = maze.await()
        val w = wikiSeries.await()
        if (f.isFailure && m.isFailure && w.isFailure) throw f.exceptionOrNull() ?: IOException("No source reachable")

        // TVMaze wins when both know a show (better poster); Wikipedia-only shows, e.g. regional ones, stay.
        val mazeShows = m.getOrDefault(emptyList())
        val known = mazeShows.map { key(it.title) }.toSet()
        val series = mazeShows + w.getOrDefault(emptyList()).filter { key(it.title) !in known }
        interleave(f.getOrDefault(emptyList()).take(6), series.take(8))
    }

    private fun key(title: String) = title.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun <T> interleave(a: List<T>, b: List<T>): List<T> = buildList {
        for (i in 0 until max(a.size, b.size)) {
            a.getOrNull(i)?.let(::add)
            b.getOrNull(i)?.let(::add)
        }
    }

    /**
     * Exact search first; when it finds fewer than three pages, retry with `word~` (CirrusSearch fuzzy matching) on the longer
     * words so transliterated titles still match, e.g. "dar e nijjat" finds "Dar-e-Nijaat".
     */
    private suspend fun wikipedia(query: String, template: String, kind: WatchKind): List<WatchResult> {
        val exact = wikipediaQuery("$query hastemplate:\"$template\"", kind)
        if (exact.size >= 3) return exact
        val fuzzy = query.trim().split(Regex("\\s+")).joinToString(" ") { if (it.length >= 4) "$it~" else it }
        if (fuzzy == query.trim()) return exact
        return (exact + wikipediaQuery("$fuzzy hastemplate:\"$template\"", kind)).distinctBy { it.title + it.year }
    }

    private suspend fun wikipediaQuery(searchTerm: String, kind: WatchKind): List<WatchResult> = withContext(Dispatchers.IO) {
        val term = URLEncoder.encode(searchTerm, "UTF-8")
        val url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$term&gsrlimit=10" +
            "&prop=pageprops%7Cdescription&ppprop=page_image%7Cpage_image_free&format=json&formatversion=2"
        val pages = JSONObject(String(WikipediaGames.get(url))).optJSONObject("query")?.optJSONArray("pages") ?: return@withContext emptyList()
        List(pages.length()) { pages.getJSONObject(it) }
            .sortedBy { it.optInt("index") }
            .mapNotNull { page ->
                val description = page.optString("description")
                if (kind == WatchKind.Series && (!tvWords.containsMatchIn(description) || notTv.containsMatchIn(description))) return@mapNotNull null
                val props = page.optJSONObject("pageprops")
                val image = props?.optString("page_image").orEmpty().ifEmpty { props?.optString("page_image_free").orEmpty() }
                val cover = image.takeIf { it.isNotEmpty() }?.let(WikipediaGames::fileUrl)
                val rawTitle = page.optString("title")
                WatchResult(
                    kind = kind,
                    title = rawTitle.replace(trailingBrackets, ""),
                    year = (yearPattern.find(description) ?: yearPattern.find(rawTitle))?.value?.toInt(),
                    info = if (kind == WatchKind.Movie) directedBy.find(description)?.groupValues?.get(1).orEmpty() else description,
                    coverUrl = cover,
                    thumbUrl = cover,
                    source = "Wikipedia",
                )
            }
    }

    private suspend fun tvMaze(query: String): List<WatchResult> = withContext(Dispatchers.IO) {
        val results = JSONArray(String(WikipediaGames.get("https://api.tvmaze.com/search/shows?q=${URLEncoder.encode(query, "UTF-8")}")))
        List(minOf(results.length(), 8)) { results.getJSONObject(it).getJSONObject("show") }.map { show ->
            val image = show.optJSONObject("image")
            val channel = (show.optJSONObject("webChannel") ?: show.optJSONObject("network"))?.string("name")
            val genres = show.optJSONArray("genres")?.let { g -> List(minOf(g.length(), 2)) { g.getString(it) } }.orEmpty()
            WatchResult(
                kind = WatchKind.Series,
                title = show.optString("name"),
                year = show.string("premiered")?.take(4)?.toIntOrNull(),
                info = (listOfNotNull(channel) + genres).joinToString(" · "),
                coverUrl = image?.string("original"),
                thumbUrl = image?.string("medium"),
                source = "TVMaze",
            )
        }.filter { it.title.isNotBlank() }
    }

    // org.json returns the text "null" from optString for a JSON null.
    private fun JSONObject.string(key: String): String? = if (isNull(key)) null else getString(key).ifBlank { null }
}
