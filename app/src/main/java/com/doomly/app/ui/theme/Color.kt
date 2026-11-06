package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val LightVoid = Color(0xFFF8F3E7)
internal val LightPanel = Color(0xFFEEE8D8)
internal val LightPanelRaised = Color(0xFFE1E7D6)
internal val LightHairline = Color(0xFFC9CABB)
internal val LightFrost = Color(0xFF203D32)
internal val LightSmoke = Color(0xFF5F665B)
internal val LightDanger = Color(0xFF9F3B36)

internal val DarkVoid = Color(0xFF211E19)
internal val DarkPanel = Color(0xFF2C2822)
internal val DarkPanelRaised = Color(0xFF383229)
internal val DarkHairline = Color(0xFF61574A)
internal val DarkFrost = Color(0xFFF8F0E2)
internal val DarkSmoke = Color(0xFFC0B29F)
internal val DarkDanger = Color(0xFFFF8A80)

internal val DoomlyMint = Color(0xFFA4472D)
internal val DoomlyAqua = Color(0xFFE0E9DD)
internal val DoomlyPeach = Color(0xFFEED597)
internal val DoomlyViolet = Color(0xFFEBD6C6)

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
