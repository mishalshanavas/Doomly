package com.doomly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, Hairline.copy(alpha = .7f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun PageHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Frost)
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        trailing?.let {
            Spacer(Modifier.width(12.dp))
            it()
        }
    }
}

@Composable
fun SectionLabel(text: String, action: String? = null) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Frost)
        action?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Muted) }
    }
}

/** The dotted orbit surrounds Doomly's pixel-eyed mascot from the launcher icon. */
@Composable
fun DottedOrbit(progress: Float, modifier: Modifier = Modifier, label: String = "Daily progress") {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier.semantics { contentDescription = "$label, ${(p * 100).toInt()} percent" }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val rings = 5
        repeat(rings) { ring ->
            val count = 14 + ring * 8
            val radius = size.minDimension * (.16f + ring * .075f)
            repeat(count) { index ->
                val angle = -PI / 2 + 2 * PI * index / count
                val normalized = (ring + index.toFloat() / count) / rings
                val active = normalized <= p
                drawCircle(
                    color = if (active) Frost else Smoke.copy(alpha = .38f),
                    radius = if (active) 2.05f else 1.45f,
                    center = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )
                )
            }
        }
        drawPixelDoomly(center, size.minDimension * .29f)
    }
}

@Composable
fun DoomAvatar(@Suppress("UNUSED_PARAMETER") progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Doomly mascot" }) {
        drawPixelDoomly(Offset(size.width / 2f, size.height / 2f), size.minDimension)
    }
}

/** Draws the icon character as a crisp 17×17 pixel matrix at any Compose size. */
private fun DrawScope.drawPixelDoomly(center: Offset, diameter: Float) {
    val gridSize = 17
    val cell = diameter / gridSize
    val pixelSize = cell * .72f
    val radius = diameter / 2f

    drawCircle(Void, radius, center)
    drawCircle(Hairline.copy(alpha = .8f), radius, center, style = Stroke(cell * .28f))

    repeat(gridSize) { row ->
        repeat(gridSize) { column ->
            val x = center.x + (column - (gridSize - 1) / 2f) * cell
            val y = center.y + (row - (gridSize - 1) / 2f) * cell
            val insideFace = (x - center.x) * (x - center.x) +
                (y - center.y) * (y - center.y) <= (radius - cell * .55f) * (radius - cell * .55f)
            if (!insideFace) return@repeat

            val leftEye = isEyePixel(column, row, centerColumn = 4.5f)
            val rightEye = isEyePixel(column, row, centerColumn = 11.5f)
            drawRect(
                color = if (leftEye || rightEye) Frost else PanelRaised.copy(alpha = .72f),
                topLeft = Offset(x - pixelSize / 2f, y - pixelSize / 2f),
                size = Size(pixelSize, pixelSize)
            )
        }
    }
}

private fun isEyePixel(column: Int, row: Int, centerColumn: Float): Boolean {
    val dx = kotlin.math.abs(column - centerColumn)
    val dy = kotlin.math.abs(row - 8)
    val outerEye = dx <= 2f && dy <= 4 && !(dx > 1f && dy == 4)
    val hollowCenter = dx < 1f && dy <= 2
    return outerEye && !hollowCenter
}

@Composable
fun StatusPill(text: String) {
    Row(
        Modifier.clip(CircleShape).background(Panel).border(1.dp, Hairline, CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Frost, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatusDot(done: Boolean) {
    Box(
        Modifier.size(28.dp).clip(CircleShape)
            .background(if (done) Frost else PanelRaised)
            .border(1.dp, if (done) Frost else Hairline, CircleShape),
        Alignment.Center
    ) { Text(if (done) "✓" else "", color = Void, style = MaterialTheme.typography.labelMedium) }
}
