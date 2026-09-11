package dev.krydo.mobile.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Forced dark Stellar palette so the Android app matches the Krydo website
 * and Stitch Stellar Blue designs regardless of system light/dark preference.
 */
private val KrydoDarkColors = darkColorScheme(
    primary = KrydoColors.ElectricBlue,
    onPrimary = KrydoColors.TextPrimary,
    primaryContainer = KrydoColors.StellarBlue,
    onPrimaryContainer = KrydoColors.TextPrimary,
    secondary = KrydoColors.Cyan,
    onSecondary = KrydoColors.BackgroundPrimary,
    secondaryContainer = KrydoColors.SurfaceElevated,
    onSecondaryContainer = KrydoColors.TextSecondary,
    tertiary = KrydoColors.Violet,
    onTertiary = KrydoColors.TextPrimary,
    tertiaryContainer = Color(0xFF1E1B4B),
    onTertiaryContainer = KrydoColors.TextSecondary,
    background = KrydoColors.BackgroundPrimary,
    onBackground = KrydoColors.TextPrimary,
    surface = KrydoColors.SurfaceElevated,
    onSurface = KrydoColors.TextPrimary,
    surfaceVariant = KrydoColors.CardSurface,
    onSurfaceVariant = KrydoColors.TextSecondary,
    surfaceTint = KrydoColors.ElectricBlue,
    inverseSurface = KrydoColors.TextPrimary,
    inverseOnSurface = KrydoColors.BackgroundPrimary,
    inversePrimary = KrydoColors.BrightBlue,
    outline = KrydoColors.BorderSubtle,
    outlineVariant = KrydoColors.BorderBlue,
    error = KrydoColors.Error,
    onError = KrydoColors.TextPrimary,
    errorContainer = Color(0xFF3F1219),
    onErrorContainer = Color(0xFFFECACA),
    scrim = Color(0xCC090B12),
)

@Composable
fun KrydoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = KrydoColors.BackgroundPrimary.toArgb()
            window.navigationBarColor = KrydoColors.BackgroundPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = KrydoDarkColors,
        typography = KrydoTypography,
        shapes = KrydoShapes,
        content = content,
    )
}
