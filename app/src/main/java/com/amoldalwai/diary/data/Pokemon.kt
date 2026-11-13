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
    val caughtLevel: Int,
    val caughtAt: Long,
)

/** Names shown on cards and tiles. */
object PokemonMath {
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

/** PokéAPI (names, stats, text) and the PokeAPI sprites repository (pictures). Free, no key; callers cache what they fetch. */
object PokeApi {
    private const val API = "https://pokeapi.co/api/v2"
    private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"
    private const val PICTURE_PX = 192
    private const val PICTURE_QUALITY = 65

    private fun artworkUrl(id: Int) = "$SPRITES/other/official-artwork/$id.png"

    fun isOnline(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Downloads one Pokémon and its picture, shrunk to a small lossy WebP (192 px). Throws when anything is missing or the network fails. */
    suspend fun fetch(id: Int, level: Int): Pair<CaughtPokemon, ByteArray> = withContext(Dispatchers.IO) {
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
        CaughtPokemon(
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
            caughtLevel = level,
            caughtAt = System.currentTimeMillis(),
        ) to picture
    }

    private fun shrink(png: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(png, 0, png.size) ?: throw IOException("Unreadable picture")
        val scaled = if (source.width > PICTURE_PX) Bitmap.createScaledBitmap(source, PICTURE_PX, PICTURE_PX * source.height / source.width, true) else source
        return java.io.ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, PICTURE_QUALITY, it) }.toByteArray()
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

    /** The Poké Ball draw replaced the level unlocks, so everything caught the old way (and its larger pictures) is cleared once. */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.delete("pokemon", null, null)
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun caught(): List<CaughtPokemon> = withContext(Dispatchers.IO) {
        buildList {
            db.rawQuery(
                "SELECT id, name, types, stats, height, weight, ability, genus, flavor, legendary, caught_level, caught_at FROM pokemon ORDER BY caught_at DESC",
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
                            caughtLevel = it.getInt(10),
                            caughtAt = it.getLong(11),
                        ),
                    )
                }
            }
        }
    }

    suspend fun save(p: CaughtPokemon, picture: ByteArray) = withContext(Dispatchers.IO) {
        imageFile(p.id).writeBytes(picture)
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
