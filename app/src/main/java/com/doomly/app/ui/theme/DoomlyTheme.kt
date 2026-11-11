package com.doomly.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** 4dp grid. Sheet text and body text share one left edge (gutter + inset = screen). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val gutter = 8.dp   // sheet edge to inset card
    val inset = 12.dp   // inset card edge to text
    val screen = 20.dp  // screen edge to text
}

/** Sheet 32dp; inset card inside it 32 - 8 = 24dp; body cards 16dp; dialogs 28dp. */
val SheetRadius = 32.dp
val InsetRadius = SheetRadius - Spacing.gutter

val DoomlyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightScheme = lightColorScheme(
    primary = Palette.AccentLight, onPrimary = Color.White,
    secondary = Palette.InkLight, onSecondary = Color.White,
    secondaryContainer = Palette.FieldLight, onSecondaryContainer = Palette.InkLight,
    background = Palette.SheetLight, onBackground = Palette.InkLight,
    surface = Palette.SheetLight, onSurface = Palette.InkLight,
    surfaceVariant = Palette.InsetLight, onSurfaceVariant = Palette.SecondaryLight,
    surfaceContainerHigh = Palette.SheetLight, surfaceContainerLow = Palette.SheetLight,
    outline = Palette.SeparatorLight, outlineVariant = Palette.FieldLight,
    error = Palette.DangerLight, onError = Color.White,
    surfaceTint = Color.Transparent
)

private val DarkScheme = darkColorScheme(
    primary = Palette.AccentDark, onPrimary = Color.Black,
    secondary = Palette.InkDark, onSecondary = Color.Black,
    secondaryContainer = Palette.FieldDark, onSecondaryContainer = Palette.InkDark,
    background = Color.Black, onBackground = Palette.InkDark,
    surface = Palette.SheetDark, onSurface = Palette.InkDark,
    surfaceVariant = Palette.InsetDark, onSurfaceVariant = Palette.SecondaryDark,
    surfaceContainerHigh = Palette.SheetDark, surfaceContainerLow = Palette.SheetDark,
    outline = Palette.SeparatorDark, outlineVariant = Palette.FieldDark,
    error = Palette.DangerDark, onError = Color.White,
    surfaceTint = Color.Transparent
)

@Composable
fun DoomlyTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        val insets = WindowCompat.getInsetsController(window, view)
        // Status bar sits on the sheet; the navigation bar always sits on the black body.
        insets.isAppearanceLightStatusBars = !darkTheme
        insets.isAppearanceLightNavigationBars = false
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = DoomlyTypography,
        shapes = DoomlyShapes,
        content = { CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion(), content = content) }
    )
}
