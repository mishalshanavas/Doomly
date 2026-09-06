package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val LightVoid = Color(0xFFF9F9F6)
internal val LightPanel = Color(0xFFF1F1EE)
internal val LightPanelRaised = Color(0xFFE8E8E3)
internal val LightHairline = Color(0xFFDEDED8)
internal val LightFrost = Color(0xFF1B1B19)
internal val LightSmoke = Color(0xFF76766F)
internal val LightDanger = Color(0xFF9F3B36)

internal val DarkVoid = Color(0xFF090A09)
internal val DarkPanel = Color(0xFF141513)
internal val DarkPanelRaised = Color(0xFF20211E)
internal val DarkHairline = Color(0xFF343630)
internal val DarkFrost = Color(0xFFF3F3ED)
internal val DarkSmoke = Color(0xFFA5A69D)
internal val DarkDanger = Color(0xFFFF8A80)

internal val DoomlyMint = Color(0xFFB7D957)
internal val DoomlyAqua = Color(0xFFCFDDD8)
internal val DoomlyPeach = Color(0xFFEFC8AE)
internal val DoomlyViolet = Color(0xFFD6D0E5)

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
