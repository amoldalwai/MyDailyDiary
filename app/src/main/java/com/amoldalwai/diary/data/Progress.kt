package com.amoldalwai.diary.data

/** Everything the points, levels and awards are worked out from; all of it is read from the user's own data. */
data class ProgressStats(
    val entries: Int = 0,
    /** Photos counted for points: at most [Progress.PHOTOS_PER_ENTRY] per entry. */
    val photos: Int = 0,
    val bestStreak: Int = 0,
    val perfectHabitDays: Int = 0,
    val booksFinished: Int = 0,
    val gamesCompleted: Int = 0,
    val moviesWatched: Int = 0,
    val seriesFinished: Int = 0,
    val ratings: Int = 0,
    val reviews: Int = 0,
    val libraryItems: Int = 0,
)

enum class AwardTier(val label: String, val emoji: String) {
    Bronze("Bronze", "🥉"),
    Silver("Silver", "🥈"),
    Gold("Gold", "🥇"),
}

class Award(
    val id: String,
    val emoji: String,
    val name: String,
    val goal: String,
    val tiers: List<Int>,
    val value: (ProgressStats) -> Int,
)

/** How far along an award is: [tier] is the highest one reached (null = none yet) and [next] the target for the one after. */
data class AwardStatus(val award: Award, val current: Int, val tier: AwardTier?, val next: Int?) {
    val progressToNext: Float
        get() {
            val target = next ?: return 1f
            val floor = tier?.let { award.tiers[it.ordinal] } ?: 0
            return ((current - floor).toFloat() / (target - floor).coerceAtLeast(1)).coerceIn(0f, 1f)
        }
}

data class LevelInfo(
    val level: Int,
    val points: Int,
    val levelStart: Int,
    /** Points needed for the next level; null at the top level. */
    val nextAt: Int?,
    val title: String,
) {
    val progress: Float
        get() = if (nextAt == null) 1f else ((points - levelStart).toFloat() / (nextAt - levelStart)).coerceIn(0f, 1f)
}

/** A group of five levels with its own name, badge and colours ([from] and [to] are ARGB). */
class Rank(val name: String, val emoji: String, val from: Long, val to: Long)

object Progress {
    const val PHOTOS_PER_ENTRY = 5
    const val MAX_LEVEL = 50

    fun points(s: ProgressStats): Int =
        s.entries * 10 +
            s.photos * 2 +
            s.reviews * 10 +
            s.ratings * 5 +
            s.perfectHabitDays * 15 +
            (s.bestStreak / 7) * 50 +
            s.booksFinished * 50 +
            s.gamesCompleted * 50 +
            s.moviesWatched * 20 +
            s.seriesFinished * 40

    /** Points needed to reach [level]: 100 for level 2, then each level costs more than the last, so the top takes years. */
    fun pointsFor(level: Int): Int = 40 * (level - 1) * (level - 1) + 60 * (level - 1)

    fun entryPoints(photos: Int): Int = 10 + minOf(photos, PHOTOS_PER_ENTRY) * 2

    const val LEVELS_PER_RANK = 5

    /** Ten ranks of five levels each; every rank unlocks a profile banner and a new frame colour. */
    val ranks = listOf(
        Rank("Seedling", "🌱", 0xFF8FB996, 0xFF4F7A5A),
        Rank("Sprout", "🌿", 0xFF7BC8A4, 0xFF2F8F6B),
        Rank("Explorer", "🧭", 0xFF6EA8E5, 0xFF2F63B0),
        Rank("Storyteller", "📖", 0xFFC09AE8, 0xFF7648B8),
        Rank("Chronicler", "📜", 0xFFF28DA6, 0xFFC2456B),
        Rank("Wordsmith", "✒️", 0xFFF7A35C, 0xFFD9622B),
        Rank("Keeper of Days", "🗝️", 0xFFF7C948, 0xFFD39A12),
        Rank("Sage", "🔮", 0xFF5CD6D0, 0xFF1F8F9B),
        Rank("Legend", "🏆", 0xFFFFD86B, 0xFFE0881F),
        Rank("Mythic", "👑", 0xFFFF7BD5, 0xFF7A3CFF),
    )

    fun rankIndex(level: Int): Int = ((level - 1) / LEVELS_PER_RANK).coerceIn(0, ranks.lastIndex)

    fun rankOf(level: Int): Rank = ranks[rankIndex(level)]

    fun titleFor(level: Int): String = rankOf(level).name

    fun levelFor(points: Int, minLevel: Int = 1): LevelInfo {
        var level = 1
        while (level < MAX_LEVEL && points >= pointsFor(level + 1)) level++
        level = maxOf(level, minLevel).coerceAtMost(MAX_LEVEL)
        return LevelInfo(
            level = level,
            points = points,
            levelStart = pointsFor(level).coerceAtMost(points),
            nextAt = if (level >= MAX_LEVEL) null else pointsFor(level + 1),
            title = titleFor(level),
        )
    }

    val awards = listOf(
        Award("bookaholic", "📚", "Bookaholic", "books finished", listOf(3, 10, 25)) { it.booksFinished },
        Award("game_expert", "🎮", "Game Expert", "games completed", listOf(3, 10, 25)) { it.gamesCompleted },
        Award("movie_buff", "🎬", "Movie Buff", "movies watched", listOf(5, 20, 50)) { it.moviesWatched },
        Award("binge_master", "📺", "Binge Master", "series finished", listOf(3, 10, 25)) { it.seriesFinished },
        Award("diarist", "✍️", "Diarist", "entries written", listOf(7, 30, 100)) { it.entries },
        Award("streak_keeper", "🔥", "Streak Keeper", "day streak", listOf(3, 7, 30)) { it.bestStreak },
        Award("photographer", "📷", "Photographer", "photos added", listOf(10, 50, 200)) { it.photos },
        Award("critic", "⭐", "Critic", "ratings and reviews", listOf(3, 15, 50)) { it.ratings + it.reviews },
        Award("habit_hero", "✅", "Habit Hero", "perfect habit days", listOf(3, 15, 60)) { it.perfectHabitDays },
        Award("collector", "🗂️", "Collector", "items in your library", listOf(10, 30, 100)) { it.libraryItems },
    )

    fun statuses(s: ProgressStats): List<AwardStatus> = awards.map { award ->
        val value = award.value(s)
        val reached = award.tiers.indexOfLast { value >= it }
        AwardStatus(
            award = award,
            current = value,
            tier = AwardTier.entries.getOrNull(reached),
            next = award.tiers.getOrNull(reached + 1),
        )
    }

    /** Ids of every award tier reached, like `bookaholic:0` (bronze) and `bookaholic:1` (silver). */
    fun reachedKeys(s: ProgressStats): Set<String> = buildSet {
        statuses(s).forEach { st ->
            val tier = st.tier ?: return@forEach
            for (i in 0..tier.ordinal) add("${st.award.id}:$i")
        }
    }
}
