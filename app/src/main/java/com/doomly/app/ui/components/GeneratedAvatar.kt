package com.doomly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import java.security.MessageDigest

/** A stable, mirrored identicon generated locally from an account or device identity. */
@Composable
fun GeneratedAvatar(seed: String, modifier: Modifier = Modifier) {
    val digest = remember(seed) { MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8)) }
    val colors = listOf(lerp(Mint, Frost, .32f), lerp(Mint, Frost, .65f), lerp(Violet, Frost, .62f))
    val foreground = colors[(digest[15].toInt() and 0xff) % colors.size]
    val accent = lerp(foreground, Mint, .5f)
    val shape = RoundedCornerShape(8.dp)
    Canvas(modifier.clip(shape).background(Mint.copy(alpha = .1f))
        .border(1.dp, Hairline.copy(alpha = .2f), shape)
        .padding(11.dp).semantics { contentDescription = "Generated geometric avatar" }) {
        val side = size.minDimension
        val cell = side / 5f
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        repeat(5) { row ->
            repeat(3) { column ->
                val value = digest[row * 3 + column].toInt() and 0xff
                if (value and 1 == 1 || (row == 2 && column == 2)) {
                    val color = if (value and 2 == 2) foreground else accent
                    val columns = if (column == 2) listOf(column) else listOf(column, 4 - column)
                    columns.forEach { mirrored ->
                        drawRoundRect(color, origin + Offset(mirrored * cell + cell * .05f, row * cell + cell * .05f),
                            Size(cell * .9f, cell * .9f), CornerRadius(cell * .08f))
                    }
                }
            }
        }
    }
}
