package com.amoldalwai.diary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LightColors = lightColorScheme(
    primary = Color(0xFFD9775A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D6),
    onPrimaryContainer = Color(0xFF5A2312),
    secondary = Color(0xFF7C9A82),
    onSecondary = Color.White,
    tertiary = Color(0xFF8B7BB8),
    background = Color(0xFFFBF6EF),
    onBackground = Color(0xFF2E2622),
    surface = Color(0xFFFFFCF8),
    onSurface = Color(0xFF2E2622),
    surfaceVariant = Color(0xFFF3EADF),
    onSurfaceVariant = Color(0xFF6E625A),
    outline = Color(0xFFD9CCBF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0A58A),
    onPrimary = Color(0xFF4A1C0C),
    primaryContainer = Color(0xFF6B3320),
    onPrimaryContainer = Color(0xFFFFE1D6),
    secondary = Color(0xFF9DB8A3),
    tertiary = Color(0xFFB3A5DA),
    background = Color(0xFF1A1613),
    onBackground = Color(0xFFF1E8DD),
    surface = Color(0xFF241F1B),
    onSurface = Color(0xFFF1E8DD),
    surfaceVariant = Color(0xFF342D27),
    onSurfaceVariant = Color(0xFFC9BDB2),
    outline = Color(0xFF5A4F47),
)

private val base = Typography()

private val DiaryTypography = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
)

@Composable
fun DiaryTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiaryTypography,
        content = content,
    )
}
