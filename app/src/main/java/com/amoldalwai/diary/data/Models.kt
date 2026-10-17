package com.amoldalwai.diary.data

import java.time.LocalDate
import java.util.UUID

enum class Mood(val emoji: String, val label: String) {
    Happy("😊", "Happy"),
    Loved("🥰", "Loved"),
    Excited("🤩", "Excited"),
    Calm("😌", "Calm"),
    Tired("😴", "Tired"),
    Sad("😢", "Sad"),
    Anxious("😰", "Anxious"),
    Angry("😤", "Angry"),
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
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank() && images.isEmpty()
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
}
