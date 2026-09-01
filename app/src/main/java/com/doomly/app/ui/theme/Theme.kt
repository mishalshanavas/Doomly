package com.doomly.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Scheme = lightColorScheme(
    primary = Ink, onPrimary = Card,
    primaryContainer = Sunshine, onPrimaryContainer = Ink,
    secondary = Coral, onSecondary = Card,
    secondaryContainer = SunshineSoft, onSecondaryContainer = Ink,
    background = Paper, onBackground = Ink,
    surface = Card, onSurface = Ink,
    surfaceVariant = Color(0xFFF5F0E6), onSurfaceVariant = Muted,
    outline = Line, outlineVariant = Line,
    error = Coral, onError = Card
)

@Composable
fun DoomlyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
    }
    MaterialTheme(colorScheme = Scheme, typography = DoomlyTypography, content = content)
}
