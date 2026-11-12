package com.amoldalwai.diary.data

import java.time.LocalDate
import java.util.UUID

/** [emoji] is only a text stand-in (PDF export, notifications); the app draws the matching face image. */
enum class Mood(val emoji: String, val label: String) {
    VeryHappy("😄", "Very Happy"),
    Happy("🙂", "Happy"),
    Calm("😌", "Calm"),
    Neutral("😐", "Neutral"),
    Sad("🙁", "Sad"),
    VerySad("😭", "Very Sad"),
    Angry("😠", "Angry"),
    Stressed("😣", "Stressed"),
    Tired("😴", "Tired"),
    Excited("🤩", "Excited"),
}

data class PageStyle(
    val backgroundId: String = "paper",
    val fontId: String = "serif",
    val fontScale: Float = 1f,
    val lines: String = "ruled",
    val lineColorId: String = "auto",
    val margin: Boolean = true,
)

enum class SpanType { Bold, Italic }

/** Formatting over a range of the plain-text body; [end] is exclusive. */
data class TextSpan(val start: Int, val end: Int, val type: SpanType)

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deleted: Boolean = false,
)

/** One day's tick for a habit; [done] = false is kept so an un-tick can sync to other phones. */
data class HabitCheck(val done: Boolean, val updatedAt: Long)

data class HabitStat(
    val habit: Habit,
    val currentStreak: Int,
    val bestStreak: Int,
    val doneLast30: Int,
    val totalDone: Int,
    val last28: List<Boolean>,
)

enum class BookStatus { Want, Reading, Finished }

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val openLibraryKey: String? = null,
    val title: String,
    val authors: String = "",
    val year: Int? = null,
    val cover: String? = null,
    val totalPages: Int? = null,
    val currentPage: Int = 0,
    val status: BookStatus = BookStatus.Reading,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    /** Year it was finished when only the year is known; with neither this nor [finishedAt] it was finished long ago. */
    val finishedYear: Int? = null,
    val rating: Int? = null,
    val review: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
) {
    val progress: Float
        get() = totalPages?.takeIf { it > 0 }?.let { (currentPage.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
}

/** The page a book was on at the end of a given day. */
data class BookLog(val page: Int, val updatedAt: Long)

enum class GameStatus { Playing, Completed, Backlog }

/** What the user says when adding something already finished: a rating and the year ([year] null = a long time ago). */
data class FinishInfo(val rating: Int, val year: Int?)

enum class WatchKind { Movie, Series }

enum class WatchStatus { Watching, Watched, Watchlist }

/** A movie or a series; [season] and [episode] only mean something for series and 0 means not started. */
data class WatchItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: WatchKind,
    val title: String,
    val year: Int? = null,
    val info: String = "",
    val cover: String? = null,
    val status: WatchStatus = WatchStatus.Watched,
    val season: Int = 0,
    val episode: Int = 0,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val finishedYear: Int? = null,
    val rating: Int? = null,
    val review: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
)

data class Game(
    val id: String = UUID.randomUUID().toString(),
    val steamAppId: Long? = null,
    val title: String,
    val developer: String = "",
    val year: Int? = null,
    val platform: String = "PC",
    val cover: String? = null,
    val status: GameStatus = GameStatus.Playing,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val completedYear: Int? = null,
    val rating: Int? = null,
    val review: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false,
)

data class DiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val title: String = "",
    val body: String = "",
    val mood: Mood? = null,
    val style: PageStyle = PageStyle(),
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val spans: List<TextSpan> = emptyList(),
    /** Library items attached to this page, as `B:<id>` (book), `G:<id>` (game) or `W:<id>` (movie/series). */
    val links: List<String> = emptyList(),
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank() && images.isEmpty() && links.isEmpty()

    /** A page can only be saved with a title or some writing. */
    val hasText: Boolean get() = title.isNotBlank() || body.isNotBlank()
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
}
