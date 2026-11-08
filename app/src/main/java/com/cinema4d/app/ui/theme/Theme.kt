package com.cinema4d.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** True when the app is showing its daylight (sun) appearance. */
val LocalCinemaLight = staticCompositionLocalOf { false }

private val CinemaColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentDim,
    onPrimaryContainer = AccentBright,
    secondary = Steel400,
    onSecondary = Color(0xFF0E1420),
    secondaryContainer = Color(0xFF1B2740),
    onSecondaryContainer = Steel400,
    tertiary = Gold400,
    background = Ink800,
    onBackground = Color(0xFFE8EAF0),
    surface = Ink700,
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Ink600,
    onSurfaceVariant = Color(0xFF9AA3B5),
    outline = Color(0xFF343B4D),
    outlineVariant = Color(0xFF232838),
    error = ErrorRed,
    onError = Color(0xFF2A0D0D),
)

private val CinemaLightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFFFFE7C2),
    onPrimaryContainer = Color(0xFF5A3A00),
    secondary = Color(0xFF5B6B85),
    onSecondary = Color(0xFFF7F9FF),
    secondaryContainer = Color(0xFFE2E8F2),
    onSecondaryContainer = Color(0xFF33415A),
    tertiary = Gold600,
    background = Color(0xFFF4F0E8),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFDFBF6),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFE9E3D7),
    onSurfaceVariant = Color(0xFF6B6459),
    outline = Color(0xFFC9C1B2),
    outlineVariant = Color(0xFFE2DCCF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

@Composable
fun Cinema4DTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCinemaLight provides !darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) CinemaColorScheme else CinemaLightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
