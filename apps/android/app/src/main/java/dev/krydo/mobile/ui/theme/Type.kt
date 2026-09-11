package dev.krydo.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Stitch / Manrope-inspired scale. Uses the platform sans stack until
 * bundled Manrope assets are added under res/font.
 */
private val KrydoFont = FontFamily.SansSerif

val KrydoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        color = KrydoColors.TextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp,
        color = KrydoColors.TextPrimary,
    ),
    headlineLarge = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
        color = KrydoColors.TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = KrydoColors.TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = KrydoColors.TextPrimary,
    ),
    titleLarge = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = KrydoColors.TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = KrydoColors.TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = KrydoColors.TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = KrydoColors.TextSecondary,
    ),
    bodyMedium = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = KrydoColors.TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = KrydoColors.TextMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
        color = KrydoColors.TextPrimary,
    ),
    labelMedium = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
        color = KrydoColors.TextMuted,
    ),
    labelSmall = TextStyle(
        fontFamily = KrydoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
        color = KrydoColors.TextMuted,
    ),
)
