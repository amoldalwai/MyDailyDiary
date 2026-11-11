package com.amoldalwai.diary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.amoldalwai.diary.data.Palette

private fun c(hex: Long) = Color(0xFF000000 or hex)

private fun light(
    primary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, secondaryContainer: Long, onSecondaryContainer: Long, tertiary: Long,
    background: Long, surface: Long, surfaceVariant: Long, onSurfaceVariant: Long, outline: Long, ink: Long,
) = lightColorScheme(
    primary = c(primary), onPrimary = Color.White,
    primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    secondary = c(secondary), onSecondary = Color.White,
    secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary),
    background = c(background), onBackground = c(ink),
    surface = c(surface), onSurface = c(ink),
    surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    outline = c(outline),
)

private fun dark(
    primary: Long, onPrimary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, secondaryContainer: Long, onSecondaryContainer: Long, tertiary: Long,
    background: Long, surface: Long, surfaceVariant: Long, onSurfaceVariant: Long, outline: Long, ink: Long,
) = darkColorScheme(
    primary = c(primary), onPrimary = c(onPrimary),
    primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    secondary = c(secondary), onSecondary = c(background),
    secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary),
    background = c(background), onBackground = c(ink),
    surface = c(surface), onSurface = c(ink),
    surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    outline = c(outline),
)

/** Four palettes, each in light and dark; Sage is the default and Terracotta is the original look. */
fun diaryColors(palette: Palette, darkTheme: Boolean): ColorScheme = when (palette) {
    Palette.Sage -> if (darkTheme) {
        dark(0x8DBF9A, 0x10241A, 0x2F4A38, 0xD6EBDC, 0xA9B9CE, 0x33415A, 0xDCE6F5, 0xD9BE86, 0x141A16, 0x1E2620, 0x2A372D, 0x9DB0A2, 0x3A4A3E, 0xEAF2EC)
    } else {
        light(0x5E8C6A, 0xDCEBDD, 0x1D3524, 0x7C8FA8, 0xE0E7EF, 0x223041, 0xC9A66B, 0xF4F7F2, 0xFFFFFF, 0xE9EFE7, 0x5F6F63, 0xD5DED3, 0x1F2A22)
    }

    Palette.Teal -> if (darkTheme) {
        dark(0x4FD1CA, 0x00302D, 0x0F5A56, 0xD2F0EE, 0xF0A988, 0x5A2F1A, 0xFBE3D6, 0xB3A5DA, 0x0F1A1A, 0x172525, 0x223636, 0x9FBDBC, 0x33504F, 0xE6F4F3)
    } else {
        light(0x18A39D, 0xD2F0EE, 0x0B3A38, 0xE08A5E, 0xFBE3D6, 0x4A2414, 0x7B6BB5, 0xF4F9F9, 0xFFFFFF, 0xE4EFEF, 0x587776, 0xD3E3E2, 0x10302F)
    }

    Palette.Lavender -> if (darkTheme) {
        dark(0xB3A5DA, 0x2B2150, 0x453A73, 0xE8E2F6, 0xE3A9C4, 0x5A3447, 0xF6E1EB, 0x9DC4A7, 0x18151F, 0x221E2C, 0x2E2940, 0xB7B0CC, 0x453F5C, 0xEFEAF8)
    } else {
        light(0x7B6BB5, 0xE8E2F6, 0x2B2150, 0xC98BA8, 0xF6E1EB, 0x4A2236, 0x5E8C6A, 0xF6F4FA, 0xFFFFFF, 0xEEEAF6, 0x6F6888, 0xDDD7EA, 0x2A2540)
    }

    Palette.Terracotta -> if (darkTheme) {
        dark(0xF0A58A, 0x4A1C0C, 0x6B3320, 0xFFE1D6, 0x9DB8A3, 0x2C4533, 0xD9EBDD, 0xB3A5DA, 0x1A1613, 0x241F1B, 0x342D27, 0xC9BDB2, 0x5A4F47, 0xF1E8DD)
    } else {
        light(0xD9775A, 0xFFE1D6, 0x5A2312, 0x7C9A82, 0xE1ECE3, 0x22382A, 0x8B7BB8, 0xFBF6EF, 0xFFFCF8, 0xF3EADF, 0x6E625A, 0xD9CCBF, 0x2E2622)
    }
}

private val base = Typography()

private val DiaryTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
)

@Composable
fun DiaryTheme(darkTheme: Boolean = isSystemInDarkTheme(), palette: Palette = Palette.Sage, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = diaryColors(palette, darkTheme),
        typography = DiaryTypography,
        content = content,
    )
}
