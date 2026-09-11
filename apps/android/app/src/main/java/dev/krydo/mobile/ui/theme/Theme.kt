package dev.krydo.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF0B1220)
private val Accent = Color(0xFF1D4ED8)
private val Surface = Color(0xFFF7F8FA)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Navy,
    background = Surface,
    surface = Color.White,
    onBackground = Navy,
    onSurface = Navy,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Navy,
    secondary = Color(0xFFE2E8F0),
    background = Navy,
    surface = Color(0xFF111827),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun KrydoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
