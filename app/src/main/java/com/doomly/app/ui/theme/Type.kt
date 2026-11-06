package com.doomly.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import com.doomly.app.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Editorial = FontFamily(Font(R.font.fraunces, FontWeight.Bold))
private fun display(size: Int, line: Int) = TextStyle(fontFamily = Editorial, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = line.sp, letterSpacing = (-1).sp)

private val Sans = FontFamily.SansSerif
private fun sans(weight: FontWeight, size: Int, line: Int, spacing: Float = 0f) =
    TextStyle(fontFamily = Sans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

val DoomlyTypography = Typography(
    displayLarge = display(80, 88),
    displayMedium = display(48, 54),
    headlineLarge = display(40, 46),
    headlineMedium = display(34, 40),
    headlineSmall = sans(FontWeight.Medium, 20, 26, -.3f),
    titleLarge = sans(FontWeight.Medium, 19, 25, -.2f),
    titleMedium = sans(FontWeight.Medium, 15, 21),
    titleSmall = sans(FontWeight.Medium, 13, 18),
    bodyLarge = sans(FontWeight.Normal, 16, 24),
    bodyMedium = sans(FontWeight.Normal, 14, 21),
    bodySmall = sans(FontWeight.Normal, 12, 18),
    labelLarge = sans(FontWeight.Medium, 14, 19, .1f),
    labelMedium = sans(FontWeight.Medium, 12, 16, .2f),
    labelSmall = sans(FontWeight.Bold, 11, 16, 1.2f)
)
