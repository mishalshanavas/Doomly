package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val LightVoid = Color(0xFFFFFFFF)
internal val LightPanel = Color(0xFFE5E5E5)
internal val LightPanelRaised = Color(0xFFDFDFDF)
internal val LightHairline = Color(0xFFC5C5C5)
internal val LightFrost = Color(0xFF161616)
internal val LightSmoke = Color(0xFF666666)
internal val LightDanger = Color(0xFF5B5B5B)

internal val DarkVoid = Color(0xFF1D1D1D)
internal val DarkPanel = Color(0xFF272727)
internal val DarkPanelRaised = Color(0xFF313131)
internal val DarkHairline = Color(0xFF565656)
internal val DarkFrost = Color(0xFFEEEEEE)
internal val DarkSmoke = Color(0xFFB0B0B0)
internal val DarkDanger = Color(0xFFAEAEAE)

internal val DoomlyMint = Color(0xFF222222)
internal val DoomlyAqua = Color(0xFFE2E2E2)
internal val DoomlyPeach = Color(0xFFC9C9C9)
internal val DoomlyViolet = Color(0xFFD8D8D8)

// Semantic tokens resolve from the active light or dark system theme.
val Void: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val Panel: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val PanelRaised: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
val Hairline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val Frost: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground
val Smoke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Muted: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Danger: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val Coral: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val Mint: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
val Acid: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val Aqua: Color @Composable @ReadOnlyComposable get() = DoomlyAqua
val Peach: Color @Composable @ReadOnlyComposable get() = DoomlyPeach
val Violet: Color @Composable @ReadOnlyComposable get() = DoomlyViolet
