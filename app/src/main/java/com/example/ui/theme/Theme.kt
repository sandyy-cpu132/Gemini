package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianBg,
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeonEmerald,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianCard,
    onSecondaryContainer = NeonEmerald,
    tertiary = PrismPurple,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    error = TerminalRed
)

private val LightColorScheme = darkColorScheme(
    // Defaulting to the sleek dark terminal aesthetic as Termux / Local AI runner is fundamentally a dark developer-centric tool
    primary = CyberCyan,
    onPrimary = ObsidianBg,
    primaryContainer = CyberCyanDark,
    onPrimaryContainer = TextPrimary,
    secondary = NeonEmerald,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianCard,
    onSecondaryContainer = NeonEmerald,
    tertiary = PrismPurple,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    error = TerminalRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // default to cyber dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianBg.toArgb()
                window.navigationBarColor = ObsidianBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
