package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CleanInkBlack,
    onPrimary = Color.White,
    primaryContainer = SparkYellowLight,
    onPrimaryContainer = CleanInkBlack,
    secondary = MintTeal,
    onSecondary = Color.White,
    secondaryContainer = MintTealLight,
    onSecondaryContainer = CleanInkBlack,
    tertiary = SparkYellow,
    onTertiary = CleanInkBlack,
    tertiaryContainer = SparkYellowLight,
    onTertiaryContainer = CleanInkBlack,
    background = CleanCanvasBackground,
    onBackground = CleanCharcoalText,
    surface = CleanSurface,
    onSurface = CleanInkBlack,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = CleanMutedText,
    outline = CleanPillBorder,
    outlineVariant = CleanBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = SparkYellow,
    onPrimary = Color(0xFF141413),
    primaryContainer = DarkMinimalSurfaceVariant,
    onPrimaryContainer = DarkMinimalText,
    secondary = MintTeal,
    onSecondary = Color(0xFF141413),
    secondaryContainer = DarkMinimalSurfaceVariant,
    onSecondaryContainer = MintTeal,
    tertiary = SparkYellow,
    onTertiary = Color(0xFF141413),
    tertiaryContainer = DarkMinimalSurfaceVariant,
    onTertiaryContainer = SparkYellow,
    background = DarkMinimalBackground,
    onBackground = DarkMinimalText,
    surface = DarkMinimalSurface,
    onSurface = DarkMinimalText,
    surfaceVariant = DarkMinimalSurfaceVariant,
    onSurfaceVariant = DarkMinimalMuted,
    outline = DarkMinimalBorder,
    outlineVariant = DarkMinimalBorderSubtle
)

@Composable
fun ArtSparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
