package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Two surfaces, both monochrome:
 *  - the sheet: a light card at the top of each screen (dark grey in dark mode)
 *  - the body: a black area below it with dark cards
 * One accent (green) marks live state and completion only.
 * Secondary text uses solid colours chosen for contrast, never alpha.
 */
object Palette {
    val SheetLight = Color(0xFFFFFFFF)
    val InsetLight = Color(0xFFF2F2F4)
    val FieldLight = Color(0xFFE6E6E8)
    val InkLight = Color(0xFF000000)
    val SecondaryLight = Color(0xFF6E6E73)
    val SeparatorLight = Color(0xFFAEAEB2)
    val AccentLight = Color(0xFF0ACF70)
    val DangerLight = Color(0xFFFF3B30)

    val SheetDark = Color(0xFF1C1C1E)
    val InsetDark = Color(0xFF2C2C2E)
    val FieldDark = Color(0xFF3A3A3C)
    val InkDark = Color(0xFFFFFFFF)
    val SecondaryDark = Color(0xFF98989F)
    val SeparatorDark = Color(0xFF636366)
    val AccentDark = Color(0xFF30D158)
    val DangerDark = Color(0xFFFF453A)
}

// Body: identical in light and dark mode.
val BodyBg = Color(0xFF000000)
val BodyCard = Color(0xFF171718)
val BodyFill = Color(0xFF2C2C2E)
val BodyInk = Color(0xFFFFFFFF)
val BodyMuted = Color(0xFF98989F)
val BodySeparator = Color(0xFF2C2C2E)
val BodyAccent = Color(0xFF30D158)
val BodyDanger = Color(0xFFFF453A)

// Sheet tokens follow the active scheme.
val Ground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val Sheet: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val Surface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val SheetInset: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val Field: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
val Ink: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val Smoke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Hairline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val Accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val Danger: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
