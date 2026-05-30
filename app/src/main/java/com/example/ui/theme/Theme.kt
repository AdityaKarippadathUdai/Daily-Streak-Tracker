package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = DarkOnBg,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightOnBg,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
