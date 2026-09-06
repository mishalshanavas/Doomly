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
import androidx.compose.ui.graphics.Path
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
fun PageHeader(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Frost)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
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

/** The dotted orbit is Doomly's signature progress mark: data first, smile second. */
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
        drawCircle(Frost, size.minDimension * .105f, center)
        val leftEye = Offset(center.x - size.minDimension * .035f, center.y - size.minDimension * .018f)
        val rightEye = Offset(center.x + size.minDimension * .035f, center.y - size.minDimension * .018f)
        drawCircle(Void, size.minDimension * .009f, leftEye)
        drawCircle(Void, size.minDimension * .009f, rightEye)
        val smile = Path().apply {
            moveTo(center.x - size.minDimension * .038f, center.y + size.minDimension * .018f)
            quadraticTo(center.x, center.y + size.minDimension * .05f, center.x + size.minDimension * .038f, center.y + size.minDimension * .018f)
        }
        drawPath(smile, Void, style = Stroke(size.minDimension * .009f))
    }
}

@Composable
fun DoomAvatar(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Doomly mascot" }) {
        drawCircle(Frost, size.minDimension / 2f)
        val eyeY = size.height * .42f
        listOf(.37f, .63f).forEach { x -> drawCircle(Void, size.minDimension * .045f, Offset(size.width * x, eyeY)) }
        val mouth = Path().apply {
            moveTo(size.width * .32f, size.height * .62f)
            quadraticTo(size.width * .5f, size.height * (.72f + progress.coerceIn(0f, 1f) * .05f), size.width * .68f, size.height * .62f)
        }
        drawPath(mouth, Void, style = Stroke(size.minDimension * .045f))
    }
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
