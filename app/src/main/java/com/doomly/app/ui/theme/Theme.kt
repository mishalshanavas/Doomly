package com.doomly.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = LightFrost, onPrimary = LightVoid,
    primaryContainer = LightPanelRaised, onPrimaryContainer = LightFrost,
    secondary = LightSmoke, onSecondary = LightVoid,
    secondaryContainer = LightPanel, onSecondaryContainer = LightFrost,
    tertiary = DoomlyMint, onTertiary = LightFrost,
    background = LightVoid, onBackground = LightFrost,
    surface = LightVoid, onSurface = LightFrost,
    surfaceVariant = LightPanel, onSurfaceVariant = LightSmoke,
    outline = LightHairline, outlineVariant = LightHairline,
    error = LightDanger, onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = DarkFrost, onPrimary = DarkVoid,
    primaryContainer = DarkPanelRaised, onPrimaryContainer = DarkFrost,
    secondary = DarkSmoke, onSecondary = DarkVoid,
    secondaryContainer = DarkPanel, onSecondaryContainer = DarkFrost,
    tertiary = DoomlyMint, onTertiary = DarkVoid,
    background = DarkVoid, onBackground = DarkFrost,
    surface = DarkVoid, onSurface = DarkFrost,
    surfaceVariant = DarkPanel, onSurfaceVariant = DarkSmoke,
    outline = DarkHairline, outlineVariant = DarkHairline,
    error = DarkDanger, onError = DarkVoid
)

@Composable
fun DoomlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = DoomlyTypography,
        content = content
    )
}
