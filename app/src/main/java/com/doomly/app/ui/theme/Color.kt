package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val LightVoid = Color(0xFFF8F4EB)
internal val LightPanel = Color(0xFFF0E9DC)
internal val LightPanelRaised = Color(0xFFE8DECC)
internal val LightHairline = Color(0xFFDED3C2)
internal val LightFrost = Color(0xFF30291F)
internal val LightSmoke = Color(0xFF766B5D)
internal val LightDanger = Color(0xFF9F3B36)

internal val DarkVoid = Color(0xFF211E19)
internal val DarkPanel = Color(0xFF2C2822)
internal val DarkPanelRaised = Color(0xFF383229)
internal val DarkHairline = Color(0xFF61574A)
internal val DarkFrost = Color(0xFFF8F0E2)
internal val DarkSmoke = Color(0xFFC0B29F)
internal val DarkDanger = Color(0xFFFF8A80)

internal val DoomlyMint = Color(0xFF9B753D)
internal val DoomlyAqua = Color(0xFFE7ECE1)
internal val DoomlyPeach = Color(0xFFF2DCAC)
internal val DoomlyViolet = Color(0xFFEEDDD0)

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
