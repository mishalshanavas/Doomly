package com.doomly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

internal val LightVoid = Color(0xFFFFFFFF)
internal val LightPanel = Color(0xFFF7F7F5)
internal val LightPanelRaised = Color(0xFFF0F0ED)
internal val LightHairline = Color(0xFFE6E6E2)
internal val LightFrost = Color(0xFF171717)
internal val LightSmoke = Color(0xFF707070)
internal val LightDanger = Color(0xFF9F3B36)

internal val DarkVoid = Color(0xFF151715)
internal val DarkPanel = Color(0xFF202320)
internal val DarkPanelRaised = Color(0xFF2B302B)
internal val DarkHairline = Color(0xFF9AA29A)
internal val DarkFrost = Color(0xFFF3F3ED)
internal val DarkSmoke = Color(0xFFA5A69D)
internal val DarkDanger = Color(0xFFFF8A80)

internal val DoomlyMint = Color(0xFF0ACF70)
internal val DoomlyAqua = Color(0xFFF0F9FC)
internal val DoomlyPeach = Color(0xFFFFF8E8)
internal val DoomlyViolet = Color(0xFFFBF1F5)

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
