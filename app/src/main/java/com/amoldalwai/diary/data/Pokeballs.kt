package com.amoldalwai.diary.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

enum class Rarity(val label: String, val argb: Long) {
    Common("Common", 0xFF8FA39A),
    Rare("Rare", 0xFF4A7FB5),
    Epic("Epic", 0xFF8A55C7),
    Legendary("Legendary", 0xFFE0A020),
}

/** One kind of Poké Ball: what it costs and how likely each rarity is. [odds] are weights for Common, Rare, Epic, Legendary. */
data class BallDef(
    val id: String,
    val name: String,
    val cost: Int,
    val argb: Long,
    val odds: List<Double>,
    val guaranteeEpic: Int,
    val guaranteeLegendary: Int,
) {
    val percent: List<Double>
        get() {
            val total = odds.sum()
            return odds.map { if (total > 0) 100 * it / total else 0.0 }
        }
}

/** The whole Poké Ball economy, read from `pokeball_config.json` so the chances can be changed without touching code. */
data class PokeballConfig(
    val rareFrom: Int,
    val epicFrom: Int,
    val generationWeight: Map<Int, Double>,
    val duplicateRefund: List<Int>,
    val duplicateChance: Double,
    val balls: List<BallDef>,
) {
    fun rarityOf(bst: Int, legendary: Boolean): Rarity = when {
        legendary -> Rarity.Legendary
        bst >= epicFrom -> Rarity.Epic
        bst >= rareFrom -> Rarity.Rare
        else -> Rarity.Common
    }

    fun rarityOf(p: CaughtPokemon): Rarity = rarityOf(p.stats.values.sum(), p.legendary)

    companion object {
        private const val FILE = "pokeball_config.json"

        /** A copy of the file in the app's files folder wins over the one shipped with the app; a broken copy is ignored. */
        fun load(context: Context): PokeballConfig {
            val shipped = context.assets.open(FILE).bufferedReader().use { it.readText() }
            val custom = runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()
            return custom?.let { runCatching { parse(it) }.getOrNull() } ?: parse(shipped)
        }

        fun parse(text: String): PokeballConfig {
            val json = JSONObject(text)
            val rarity = json.getJSONObject("rarity")
            val weights = json.optJSONObject("generationWeight")
            val refunds = json.getJSONArray("duplicateRefund")
            val balls = json.getJSONArray("balls")
            require(refunds.length() == 4 && balls.length() > 0)
            return PokeballConfig(
                rareFrom = rarity.getInt("rareFrom"),
                epicFrom = rarity.getInt("epicFrom"),
                generationWeight = weights?.keys()?.asSequence()?.associate { it.toInt() to weights.getDouble(it) }.orEmpty(),
                duplicateRefund = List(4) { refunds.getInt(it) },
                duplicateChance = json.optDouble("duplicateChance", 0.2).coerceIn(0.0, 1.0),
                balls = List(balls.length()) {
                    val b = balls.getJSONObject(it)
                    val odds = b.getJSONArray("odds")
                    require(odds.length() == 4)
                    BallDef(
                        id = b.getString("id"),
                        name = b.getString("name"),
                        cost = b.getInt("cost"),
                        argb = 0xFF000000L or b.getString("color").removePrefix("#").toLong(16),
                        odds = List(4) { i -> odds.getDouble(i) },
                        guaranteeEpic = b.optInt("guaranteeEpic", 0),
                        guaranteeLegendary = b.optInt("guaranteeLegendary", 0),
                    ).also { ball -> require(ball.odds.sum() > 0 && ball.cost > 0) }
                },
            )
        }
    }
}

/** Balls opened in a row without an Epic (or better) and without a Legendary; drives the guarantees. */
data class Pity(val epic: Int = 0, val legend: Int = 0)

fun pityToJson(book: Map<String, Pity>): String =
    JSONObject(book.mapValues { JSONArray(listOf(it.value.epic, it.value.legend)) }).toString()

fun pityFromJson(text: String?): Map<String, Pity> = runCatching {
    val json = JSONObject(text ?: "{}")
    json.keys().asSequence().associateWith { json.getJSONArray(it).let { a -> Pity(a.getInt(0), a.getInt(1)) } }
}.getOrDefault(emptyMap())

data class DexEntry(val id: Int, val name: String, val bst: Int, val generation: Int, val legendary: Boolean)

data class BallDraw(val entry: DexEntry, val rarity: Rarity, val pity: Pity)

/** Every Pokémon from PokéAPI (#1 to #1025) with the little needed to pick one: power, generation and the legendary flag. */
class Pokedex private constructor(private val entries: List<DexEntry>, private val config: PokeballConfig) {
    private class Pool(val items: List<DexEntry>, val cumulative: DoubleArray) {
        val total get() = cumulative.lastOrNull() ?: 0.0
    }

    private val pools: List<Pool> = Rarity.entries.map { rarity ->
        val items = entries.filter { config.rarityOf(it.bst, it.legendary) == rarity }
        var sum = 0.0
        Pool(items, DoubleArray(items.size) { i -> sum += config.generationWeight[items[i].generation] ?: 1.0; sum })
    }

    val size get() = entries.size

    /**
     * The pick is a pure function of the ball, the guarantee counters, the Pokémon already owned and the random source; nothing is saved here.
     * With the config's duplicate chance the pick is one of [owned] of the rolled rarity instead (when there is one).
     */
    fun draw(ball: BallDef, pity: Pity, random: Random, owned: Set<Int> = emptySet()): BallDraw {
        val o = ball.odds
        var tier = 0
        if (ball.guaranteeLegend(pity)) {
            tier = 3
        } else if (ball.guaranteeEpic(pity)) {
            tier = if (random.nextDouble() * (o[2] + o[3]) < o[2]) 2 else 3
        } else {
            var x = random.nextDouble() * o.sum()
            while (tier < 3 && x >= o[tier]) {
                x -= o[tier]
                tier++
            }
        }
        // An empty rarity (possible with odd thresholds in the config) falls back to the nearest one below.
        while (tier > 0 && pools[tier].items.isEmpty()) tier--
        val pool = pools[tier]
        val pity2 = Pity(if (tier >= 2) 0 else pity.epic + 1, if (tier == 3) 0 else pity.legend + 1)
        if (owned.isNotEmpty() && random.nextDouble() < config.duplicateChance) {
            val mine = pool.items.filter { it.id in owned }
            if (mine.isNotEmpty()) {
                var x = random.nextDouble() * mine.sumOf { config.generationWeight[it.generation] ?: 1.0 }
                val pick = mine.firstOrNull { x -= config.generationWeight[it.generation] ?: 1.0; x < 0 } ?: mine.last()
                return BallDraw(pick, Rarity.entries[tier], pity2)
            }
        }
        val x = random.nextDouble() * pool.total
        var lo = 0
        var hi = pool.items.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (pool.cumulative[mid] > x) hi = mid else lo = mid + 1
        }
        return BallDraw(
            entry = pool.items[lo],
            rarity = Rarity.entries[tier],
            pity = pity2,
        )
    }

    private fun BallDef.guaranteeLegend(p: Pity) = guaranteeLegendary > 0 && p.legend >= guaranteeLegendary - 1
    private fun BallDef.guaranteeEpic(p: Pity) = guaranteeEpic > 0 && p.epic >= guaranteeEpic - 1

    companion object {
        fun load(context: Context, config: PokeballConfig): Pokedex {
            val rows = JSONArray(context.assets.open("pokedex.json").bufferedReader().use { it.readText() })
            return Pokedex(
                List(rows.length()) {
                    val r = rows.getJSONArray(it)
                    DexEntry(r.getInt(0), r.getString(1), r.getInt(2), r.getInt(3), r.getInt(4) == 1)
                },
                config,
            )
        }
    }
}
