package com.amoldalwai.diary.ui

import android.graphics.Typeface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class PageBackground(
    val id: String,
    val name: String,
    val top: Color,
    val bottom: Color,
    val isDark: Boolean = false,
) {
    val ink: Color get() = if (isDark) Color(0xFFF4ECE1) else Color(0xFF2E2622)
    val inkSoft: Color get() = ink.copy(alpha = 0.6f)
    val onInk: Color get() = if (isDark) top else Color(0xFFFFFBF5)
    val brush: Brush get() = Brush.verticalGradient(listOf(top, bottom))
    val swatch: Brush get() = Brush.linearGradient(listOf(top, bottom))
}

data class DiaryFont(val id: String, val name: String, val family: FontFamily)

data class LineKind(val id: String, val name: String)

/** [color] is null for "auto", which follows the page's ink colour. */
data class LineColor(val id: String, val name: String, val color: Color?)

object PageStyles {
    val lineKinds = listOf(
        LineKind("blank", "Blank"),
        LineKind("ruled", "Ruled"),
        LineKind("dotted", "Dotted"),
        LineKind("grid", "Grid"),
    )

    val lineColors = listOf(
        LineColor("auto", "Auto", null),
        LineColor("blue", "Blue", Color(0xFF4F8FC0)),
        LineColor("rose", "Rose", Color(0xFFD9575A)),
        LineColor("green", "Green", Color(0xFF5E9A72)),
        LineColor("brown", "Brown", Color(0xFF8B6B4A)),
        LineColor("purple", "Purple", Color(0xFF8B7BB8)),
        LineColor("gold", "Gold", Color(0xFFC99A2E)),
    )

    fun lineColor(id: String, bg: PageBackground): Color {
        val preset = lineColors.firstOrNull { it.id == id }?.color
        return preset?.copy(alpha = if (bg.isDark) 0.5f else 0.38f) ?: bg.ink.copy(alpha = 0.12f)
    }

    val backgrounds = listOf(
        PageBackground("paper", "Paper", Color(0xFFFFFBF3), Color(0xFFF8EFE0)),
        PageBackground("blush", "Blush", Color(0xFFFDE9E6), Color(0xFFF8CFCB)),
        PageBackground("peach", "Peach", Color(0xFFFFE9D6), Color(0xFFFFCDAE)),
        PageBackground("butter", "Butter", Color(0xFFFFF6D2), Color(0xFFFFE8A3)),
        PageBackground("sage", "Sage", Color(0xFFE7F0E1), Color(0xFFCADFC3)),
        PageBackground("sky", "Sky", Color(0xFFE2EFFA), Color(0xFFC3DAF1)),
        PageBackground("lavender", "Lavender", Color(0xFFEEE7F9), Color(0xFFD6C9F0)),
        PageBackground("sunset", "Sunset", Color(0xFFFFE0B8), Color(0xFFF9A8B8)),
        PageBackground("aurora", "Aurora", Color(0xFFCDBBEA), Color(0xFF9FD8D8)),
        PageBackground("midnight", "Midnight", Color(0xFF1F1B3D), Color(0xFF3F3166), isDark = true),
        PageBackground("forest", "Forest", Color(0xFF14302A), Color(0xFF2C5C4B), isDark = true),
        PageBackground("ink", "Ink", Color(0xFF17171C), Color(0xFF2B2B34), isDark = true),
    )

    val fonts = listOf(
        DiaryFont("serif", "Classic", FontFamily.Serif),
        DiaryFont("sans", "Clean", FontFamily.SansSerif),
        DiaryFont("hand", "Handwritten", FontFamily.Cursive),
        DiaryFont("casual", "Casual", FontFamily(Typeface.create("casual", Typeface.NORMAL))),
        DiaryFont("light", "Light", FontFamily(Typeface.create("sans-serif-light", Typeface.NORMAL))),
        DiaryFont("mono", "Typewriter", FontFamily.Monospace),
    )

    fun background(id: String) = backgrounds.firstOrNull { it.id == id } ?: backgrounds.first()
    fun font(id: String) = fonts.firstOrNull { it.id == id } ?: fonts.first()
}
