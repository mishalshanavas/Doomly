package com.doomly.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
fun DottedOrbit(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String = "Daily progress",
    mascotScale: Float = .29f
) {
    val p = progress.coerceIn(0f, 1f)
    val backgroundColor = Void
    val foregroundColor = Frost
    val pixelColor = PanelRaised
    val outlineColor = Hairline
    val inactiveDotColor = Smoke
    val motion = rememberInfiniteTransition(label = "Doomly mascot motion")
    val orbitPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "Orbit rotation"
    )
    val breath by motion.animateFloat(
        initialValue = .98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Mascot breathing"
    )
    val eyeOpen by motion.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 4_200
            1f at 0
            1f at 3_550
            .12f at 3_650
            1f at 3_780
            1f at 4_200
        }),
        label = "Mascot blink"
    )
    Canvas(modifier.semantics { contentDescription = "$label, ${(p * 100).toInt()} percent" }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val rings = 5
        repeat(rings) { ring ->
            val count = 14 + ring * 8
            val radius = size.minDimension * (.16f + ring * .075f)
            repeat(count) { index ->
                val direction = if (ring % 2 == 0) 1f else -.72f
                val angle = -PI / 2 + 2 * PI * index / count + orbitPhase * direction
                val normalized = (ring + index.toFloat() / count) / rings
                val active = normalized <= p
                drawCircle(
                    color = if (active) foregroundColor else inactiveDotColor.copy(alpha = .38f),
                    radius = if (active) 2.05f else 1.45f,
                    center = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )
                )
            }
        }
        drawPixelDoomly(
            center = center,
            diameter = size.minDimension * mascotScale * breath,
            eyeOpen = eyeOpen,
            pupilOffset = sin(orbitPhase * .5f) * .55f,
            backgroundColor = backgroundColor,
            pixelColor = pixelColor,
            eyeColor = foregroundColor,
            outlineColor = outlineColor
        )
    }
}

@Composable
fun DoomAvatar(@Suppress("UNUSED_PARAMETER") progress: Float, modifier: Modifier = Modifier) {
    val backgroundColor = Void
    val foregroundColor = Frost
    val pixelColor = PanelRaised
    val outlineColor = Hairline
    Canvas(modifier.semantics { contentDescription = "Doomly mascot" }) {
        drawPixelDoomly(
            center = Offset(size.width / 2f, size.height / 2f),
            diameter = size.minDimension,
            backgroundColor = backgroundColor,
            pixelColor = pixelColor,
            eyeColor = foregroundColor,
            outlineColor = outlineColor
        )
    }
}

/** Draws the icon character as a crisp 17×17 pixel matrix at any Compose size. */
private fun DrawScope.drawPixelDoomly(
    center: Offset,
    diameter: Float,
    eyeOpen: Float = 1f,
    pupilOffset: Float = 0f,
    backgroundColor: Color,
    pixelColor: Color,
    eyeColor: Color,
    outlineColor: Color
) {
    val gridSize = 17
    val cell = diameter / gridSize
    val pixelSize = cell * .72f
    val radius = diameter / 2f

    drawCircle(backgroundColor, radius, center)
    drawCircle(outlineColor.copy(alpha = .8f), radius, center, style = Stroke(cell * .28f))

    repeat(gridSize) { row ->
        repeat(gridSize) { column ->
            val x = center.x + (column - (gridSize - 1) / 2f) * cell
            val y = center.y + (row - (gridSize - 1) / 2f) * cell
            val insideFace = (x - center.x) * (x - center.x) +
                (y - center.y) * (y - center.y) <= (radius - cell * .55f) * (radius - cell * .55f)
            if (insideFace) {
                drawRect(
                    color = pixelColor.copy(alpha = .72f),
                    topLeft = Offset(x - pixelSize / 2f, y - pixelSize / 2f),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }

    val open = eyeOpen.coerceIn(.12f, 1f)
    listOf(3, 10).forEach { startColumn ->
        EYE_PIXELS.forEachIndexed { row, pattern ->
            pattern.forEachIndexed { column, pixel ->
                if (pixel != '#') return@forEachIndexed
                val x = center.x + (startColumn + column - (gridSize - 1) / 2f) * cell
                val y = center.y + (row + 4 - (gridSize - 1) / 2f) * cell * open
                drawRect(
                    color = eyeColor,
                    topLeft = Offset(x - pixelSize / 2f, y - pixelSize * open / 2f),
                    size = Size(pixelSize, pixelSize * open)
                )
            }
        }
    }

    // Matching pupils move together very slightly, giving the mascot a curious gaze.
    listOf(4.5f, 11.5f).forEach { eyeCenter ->
        val pupilX = center.x + (eyeCenter + pupilOffset - (gridSize - 1) / 2f) * cell
        drawRect(
            color = eyeColor,
            topLeft = Offset(pupilX - pixelSize * .36f, center.y - pixelSize * open * .36f),
            size = Size(pixelSize * .72f, pixelSize * open * .72f)
        )
    }
}

private val EYE_PIXELS = listOf(
    ".##.",
    "####",
    "#..#",
    "#..#",
    "#..#",
    "#..#",
    "#..#",
    "####",
    ".##."
)

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
