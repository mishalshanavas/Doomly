package com.doomly.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif
private val Editorial = FontFamily.Serif
private fun sans(weight: FontWeight, size: Int, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Sans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)
private fun editorial(weight: FontWeight, size: Int, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Editorial, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val DoomlyTypography = Typography(
    displayLarge = sans(FontWeight.Normal, 54, 58, -1.8f),
    displayMedium = sans(FontWeight.Normal, 40, 44, -1.1f),
    headlineLarge = editorial(FontWeight.Normal, 32, 38, -.6f),
    headlineMedium = editorial(FontWeight.Normal, 26, 32, -.3f),
    headlineSmall = editorial(FontWeight.Normal, 21, 27),
    titleLarge = sans(FontWeight.Medium, 19, 25, -.2f),
    titleMedium = sans(FontWeight.Medium, 15, 21),
    titleSmall = sans(FontWeight.Medium, 13, 18),
    bodyLarge = sans(FontWeight.Normal, 16, 24),
    bodyMedium = sans(FontWeight.Normal, 14, 21),
    bodySmall = sans(FontWeight.Normal, 12, 18),
    labelLarge = sans(FontWeight.Medium, 14, 19, .1f),
    labelMedium = sans(FontWeight.Medium, 12, 16, .2f),
    labelSmall = sans(FontWeight.Medium, 10, 14, .35f)
)
