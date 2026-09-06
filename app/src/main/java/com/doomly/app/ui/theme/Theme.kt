package com.doomly.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Scheme = lightColorScheme(
    primary = Frost, onPrimary = Void,
    primaryContainer = PanelRaised, onPrimaryContainer = Frost,
    secondary = Smoke, onSecondary = Void,
    secondaryContainer = Panel, onSecondaryContainer = Frost,
    background = Void, onBackground = Frost,
    surface = Void, onSurface = Frost,
    surfaceVariant = Panel, onSurfaceVariant = Smoke,
    outline = Hairline, outlineVariant = Hairline,
    error = Danger, onError = Color.White
)

@Composable
fun DoomlyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
    }
    MaterialTheme(colorScheme = Scheme, typography = DoomlyTypography, content = content)
}
