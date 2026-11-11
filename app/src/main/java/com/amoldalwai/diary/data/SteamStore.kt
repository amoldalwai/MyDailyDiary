package com.amoldalwai.diary.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GameResult(
    val appId: Long,
    val title: String,
    val developer: String = "",
    val year: Int? = null,
    val coverUrl: String? = null,
    val source: String = "Steam",
)

/** Why a result was turned down, e.g. Steam lists it as "dlc" or "music". */
class NotAGameException(val type: String) : Exception("Steam lists this as \"$type\"")

/**
 * Steam storefront search and details. These endpoints are unofficial (documented by the community at
 * wiki.teamfortress.com/wiki/User:RJackson/StorefrontAPI) but need no key.
 */
object SteamStore {
    // appdetails `type` values seen in the wild: game, dlc, demo, advertising, mod, video, music, tool.
    // Car packs and other add-ons are "dlc", so only "game" is accepted; console classics come from Wikipedia.
    private val acceptedTypes = setOf("game")

    // Search returns soundtracks, mod tools and servers as plain "app" items; drop them by name.
    private val junk = Regex(
        "soundtrack|\\bost\\b|original score|artbook|art book|wallpaper|mod tools|\\bsdk\\b|dedicated server|\\bdemo\\b|playtest|\\bbeta\\b|authoring tools|digital deluxe upgrade",
        RegexOption.IGNORE_CASE,
    )

    private val yearPattern = Regex("\\b(19|20)\\d{2}\\b")

    fun portraitUrl(appId: Long) = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/library_600x900.jpg"

    fun thumbUrl(appId: Long) = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/capsule_231x87.jpg"

    fun headerUrl(appId: Long) = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/header.jpg"

    /** Search hits are checked against appdetails in parallel so add-ons, tools and soundtracks never reach the list. */
    suspend fun search(query: String): List<GameResult> = coroutineScope {
        val url = "https://store.steampowered.com/api/storesearch/?term=${URLEncoder.encode(query, "UTF-8")}&cc=us&l=en"
        val candidates = withContext(Dispatchers.IO) {
            val items = JSONObject(String(get(url))).optJSONArray("items") ?: return@withContext emptyList()
            List(items.length()) { items.getJSONObject(it) }
                .filter { it.optString("type") == "app" && !junk.containsMatchIn(it.optString("name")) }
                .map { GameResult(it.optLong("id"), it.optString("name")) }
                .filter { it.appId > 0 && it.title.isNotBlank() }
                .take(8)
        }
        candidates.map { candidate ->
            async(Dispatchers.IO) {
                try {
                    details(candidate)
                } catch (e: NotAGameException) {
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    candidate
                }
            }
        }.awaitAll().filterNotNull()
    }

    /** Fills in developer and year, and rejects anything Steam does not call a game. */
    private fun details(result: GameResult): GameResult {
        val url = "https://store.steampowered.com/api/appdetails?appids=${result.appId}&cc=us&l=en&filters=basic,release_date,developers"
        val entry = JSONObject(String(get(url))).optJSONObject(result.appId.toString())
        val data = entry?.takeIf { it.optBoolean("success") }?.optJSONObject("data") ?: return result
        val type = data.optString("type")
        if (type !in acceptedTypes) throw NotAGameException(type.ifEmpty { "unknown" })
        val developers = data.optJSONArray("developers")
        return result.copy(
            title = data.optString("name").ifBlank { result.title },
            developer = developers?.let { if (it.length() > 0) it.getString(0) else "" }.orEmpty(),
            year = yearPattern.find(data.optJSONObject("release_date")?.optString("date").orEmpty())?.value?.toInt(),
        )
    }

    /** Portrait box art, falling back to the landscape header for older apps without one. */
    suspend fun downloadCover(appId: Long): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { get(portraitUrl(appId)) }.getOrNull() ?: runCatching { get(headerUrl(appId)) }.getOrNull()
    }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            if (conn.responseCode !in 200..299) throw IOException("Steam returned ${conn.responseCode}")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}
