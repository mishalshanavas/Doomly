package com.doomly.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Sans only, light-to-regular weights. Hierarchy comes from size and colour
 * (grey label over ink title), not from bold.
 */
private val Family = FontFamily.SansSerif
private fun style(weight: FontWeight, size: Int, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Family, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val DoomlyTypography = Typography(
    displayLarge = style(FontWeight.Light, 88, 92, -3f),       // hero counter
    displayMedium = style(FontWeight.Light, 44, 50, -1.2f),    // secondary hero
    headlineLarge = style(FontWeight.Normal, 30, 36, -0.6f),
    headlineMedium = style(FontWeight.Normal, 26, 32, -0.4f),
    headlineSmall = style(FontWeight.Normal, 22, 28, -0.3f),
    titleLarge = style(FontWeight.Medium, 19, 25, -0.2f),      // stack title
    titleMedium = style(FontWeight.Medium, 17, 22, -0.1f),
    titleSmall = style(FontWeight.Medium, 15, 20),
    bodyLarge = style(FontWeight.Normal, 17, 22),
    bodyMedium = style(FontWeight.Normal, 15, 20),
    bodySmall = style(FontWeight.Normal, 13, 18),
    labelLarge = style(FontWeight.Medium, 17, 22),             // buttons
    labelMedium = style(FontWeight.Normal, 13, 18),            // section captions
    labelSmall = style(FontWeight.Normal, 11, 14)
)

/** Grey label that sits above a title at the same size. */
val TextStyle.regular: TextStyle get() = copy(fontWeight = FontWeight.Normal)

/** Fixed-width numerals so live counts and ranks do not shift as they change. */
fun tabular(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")
