package com.hhpc.smarttask.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- Data class to hold dynamic colors for the app ---
data class AppColors(
    val bgTop: Color,
    val bgMid: Color,
    val bgBot: Color,
    val glassSurface: Color,
    val glassCardBg: Color,
    val glassBorder: Color,
    val glassBorderActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        bgTop = Color.Unspecified,
        bgMid = Color.Unspecified,
        bgBot = Color.Unspecified,
        glassSurface = Color.Unspecified,
        glassCardBg = Color.Unspecified,
        glassBorder = Color.Unspecified,
        glassBorderActive = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        isDark = true
    )
}

private val DarkAppColors = AppColors(
    bgTop = DarkBgTop,
    bgMid = DarkBgMid,
    bgBot = DarkBgBot,
    glassSurface = DarkGlassSurface,
    glassCardBg = DarkGlassCardBg,
    glassBorder = DarkGlassBorder,
    glassBorderActive = DarkGlassBorderActive,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    isDark = true
)

private val LightAppColors = AppColors(
    bgTop = LightBgTop,
    bgMid = LightBgMid,
    bgBot = LightBgBot,
    glassSurface = LightGlassSurface,
    glassCardBg = LightGlassCardBg,
    glassBorder = LightGlassBorder,
    glassBorderActive = LightGlassBorderActive,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    isDark = false
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = MintAccent,
    tertiary = EmeraldLight,
    background = DarkBgTop,
    surface = DarkGlassSurface,
    onPrimary = DarkTextPrimary,
    onSecondary = DarkTextPrimary,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldDark,
    tertiary = EmeraldLight,
    background = LightBgTop,
    surface = LightGlassSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary
)

@Composable
fun SmartTaskTheme(
    themePreference: String = "SYSTEM", // "LIGHT", "DARK", "SYSTEM"
    systemIsDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themePreference) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemIsDark
    }

    val appColors = if (isDarkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = appColors.bgBot.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
