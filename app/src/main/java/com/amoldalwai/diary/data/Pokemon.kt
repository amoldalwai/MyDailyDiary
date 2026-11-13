package com.amoldalwai.diary.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A Pokémon the user has unlocked, with everything the card shows; the picture is a small file on the phone. */
data class CaughtPokemon(
    val id: Int,
    val name: String,
    val types: List<String>,
    val stats: Map<String, Int>,
    val heightM: Float,
    val weightKg: Float,
    val ability: String,
    val genus: String,
    val flavor: String,
    val legendary: Boolean,
    val cryUrl: String?,
    val caughtLevel: Int,
    val caughtAt: Long,
)

/** Which Pokémon belong to which level: three per level, in Pokédex order, so 50 levels reach #150. */
object PokemonMath {
    const val PER_LEVEL = 3
    const val PAGE_LEVELS = 5
    val total get() = PER_LEVEL * Progress.MAX_LEVEL
    val pages get() = Progress.MAX_LEVEL / PAGE_LEVELS

    fun idsFor(level: Int): IntRange = ((level - 1) * PER_LEVEL + 1)..(level * PER_LEVEL)
    fun levelOf(id: Int): Int = (id - 1) / PER_LEVEL + 1
    fun pageOf(level: Int): Int = (level - 1) / PAGE_LEVELS
    fun levelsOnPage(page: Int): IntRange = (page * PAGE_LEVELS + 1)..minOf(Progress.MAX_LEVEL, (page + 1) * PAGE_LEVELS)

    fun pretty(raw: String?): String = when {
        raw.isNullOrEmpty() -> "???"
        raw == "nidoran-f" -> "Nidoran ♀"
        raw == "nidoran-m" -> "Nidoran ♂"
        raw == "mr-mime" -> "Mr. Mime"
        raw == "farfetchd" -> "Farfetch'd"
        else -> raw.split('-').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    fun number(id: Int) = "#" + id.toString().padStart(3, '0')
}

/** A freshly downloaded Pokémon with its picture and cry. */
class Fetched(val pokemon: CaughtPokemon, val picture: ByteArray, val cry: ByteArray?)

/** PokéAPI (names, stats, text) and the PokeAPI sprites repository (pictures). Free, no key; callers cache what they fetch. */
object PokeApi {
    private const val API = "https://pokeapi.co/api/v2"
    private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"

    /** 96 px sprite, about half a kilobyte; used in the level tree. */
    fun spriteUrl(id: Int) = "$SPRITES/$id.png"
    private fun artworkUrl(id: Int) = "$SPRITES/other/official-artwork/$id.png"

    fun isOnline(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Names for the Pokédex numbers [offset]+1 .. [offset]+[limit]. */
    suspend fun names(offset: Int, limit: Int): Map<Int, String> = withContext(Dispatchers.IO) {
        val results = JSONObject(String(get("$API/pokemon?limit=$limit&offset=$offset"))).getJSONArray("results")
        buildMap {
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                put(r.getString("url").trimEnd('/').substringAfterLast('/').toInt(), r.getString("name"))
            }
        }
    }

    /** Downloads one Pokémon, its picture shrunk to 256 px and its cry (null if the cry can't be fetched). Throws when the rest is missing. */
    suspend fun fetch(id: Int, level: Int): Fetched = withContext(Dispatchers.IO) {
        val p = JSONObject(String(get("$API/pokemon/$id")))
        val s = JSONObject(String(get("$API/pokemon-species/$id")))
        val types = p.getJSONArray("types").map { it.getJSONObject("type").getString("name") }
        val stats = buildMap {
            val arr = p.getJSONArray("stats")
            for (i in 0 until arr.length()) arr.getJSONObject(i).let { put(it.getJSONObject("stat").getString("name"), it.getInt("base_stat")) }
        }
        val ability = p.getJSONArray("abilities").map { it }.firstOrNull { !it.optBoolean("is_hidden") }
            ?.getJSONObject("ability")?.getString("name").orEmpty()
        val genus = s.getJSONArray("genera").map { it }.firstOrNull { it.getJSONObject("language").getString("name") == "en" }?.getString("genus").orEmpty()
        val flavor = s.getJSONArray("flavor_text_entries").map { it }.firstOrNull { it.getJSONObject("language").getString("name") == "en" }
            ?.getString("flavor_text").orEmpty()
            .replace(Regex("[\\n\\u000c\\u00ad]"), " ").replace(Regex("\\s+"), " ").trim()
        val picture = shrink(get(artworkUrl(id)))
        val cryUrl = p.optJSONObject("cries")?.optString("latest")?.takeIf { it.isNotEmpty() }
        val pokemon = CaughtPokemon(
            id = id,
            name = p.getString("name"),
            types = types,
            stats = stats,
            heightM = p.getInt("height") / 10f,
            weightKg = p.getInt("weight") / 10f,
            ability = ability,
            genus = genus,
            flavor = flavor,
            legendary = s.optBoolean("is_legendary") || s.optBoolean("is_mythical"),
            cryUrl = cryUrl,
            caughtLevel = level,
            caughtAt = System.currentTimeMillis(),
        )
        Fetched(pokemon, picture, cryUrl?.let { downloadCry(it) })
    }

    /** The cry as a small audio file, or null when it can't be downloaded. */
    fun downloadCry(url: String): ByteArray? = runCatching { get(url) }.getOrNull()

    private fun shrink(png: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(png, 0, png.size) ?: throw IOException("Unreadable picture")
        val scaled = if (source.width > 256) Bitmap.createScaledBitmap(source, 256, 256 * source.height / source.width, true) else source
        return java.io.ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 82, it) }.toByteArray()
    }

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> = List(length()) { transform(getJSONObject(it)) }

    private fun get(url: String): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.setRequestProperty("User-Agent", WikipediaGames.USER_AGENT)
            conn.connectTimeout = 15_000
            conn.readTimeout = 25_000
            if (conn.responseCode !in 200..299) throw IOException("${conn.responseCode} from $url")
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }
}

class PokemonRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db: SQLiteDatabase get() = DiaryDb.get(appContext).writableDatabase
    private val dir = File(appContext.filesDir, "pokemon").apply { mkdirs() }

    fun imageFile(id: Int) = File(dir, "$id.webp")
    fun cryFile(id: Int) = File(dir, "$id.ogg")

    suspend fun caught(): List<CaughtPokemon> = withContext(Dispatchers.IO) {
        buildList {
            db.rawQuery(
                "SELECT id, name, types, stats, height, weight, ability, genus, flavor, legendary, cry, caught_level, caught_at FROM pokemon ORDER BY id DESC",
                null,
            ).use {
                while (it.moveToNext()) {
                    val stats = JSONObject(it.getString(3))
                    add(
                        CaughtPokemon(
                            id = it.getInt(0),
                            name = it.getString(1),
                            types = it.getString(2).split(',').filter { t -> t.isNotEmpty() },
                            stats = stats.keys().asSequence().associateWith { k -> stats.getInt(k) },
                            heightM = it.getFloat(4),
                            weightKg = it.getFloat(5),
                            ability = it.getString(6),
                            genus = it.getString(7),
                            flavor = it.getString(8),
                            legendary = it.getInt(9) == 1,
                            cryUrl = it.getString(10),
                            caughtLevel = it.getInt(11),
                            caughtAt = it.getLong(12),
                        ),
                    )
                }
            }
        }
    }

    suspend fun save(p: CaughtPokemon, picture: ByteArray, cry: ByteArray? = null) = withContext(Dispatchers.IO) {
        imageFile(p.id).writeBytes(picture)
        if (cry != null) cryFile(p.id).writeBytes(cry)
        db.insertWithOnConflict(
            "pokemon",
            null,
            ContentValues().apply {
                put("id", p.id)
                put("name", p.name)
                put("types", p.types.joinToString(","))
                put("stats", JSONObject(p.stats).toString())
                put("height", p.heightM)
                put("weight", p.weightKg)
                put("ability", p.ability)
                put("genus", p.genus)
                put("flavor", p.flavor)
                put("legendary", if (p.legendary) 1 else 0)
                put("cry", p.cryUrl)
                put("caught_level", p.caughtLevel)
                put("caught_at", p.caughtAt)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun caughtIds(): Set<Int> = withContext(Dispatchers.IO) {
        buildSet { db.rawQuery("SELECT id FROM pokemon", null).use { while (it.moveToNext()) add(it.getInt(0)) } }
    }
}
