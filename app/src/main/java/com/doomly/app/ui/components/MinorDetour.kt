package com.doomly.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.doomly.app.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*

/** A conceptual route, with the real Instagram action at its destination. */
@Composable
fun MinorDetour(onOpenInstagram: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val line by animateColorAsState(if (pressed) Mint else Hairline, tween(160), label = "Detour press")
    val startColor = Muted
    val endColor = Mint
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_coach), "Mochi, your scroll coach", Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Mochi’s coaching corner", style = MaterialTheme.typography.titleMedium, color = Frost)
                Text("Great things await. Probably in the next Reel.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("The plan", style = MaterialTheme.typography.bodySmall, color = Muted)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.Bottom) {
            Canvas(Modifier.weight(1f).heightIn(min = 92.dp).fillMaxHeight()) {
                val startX = 4.dp.toPx()
                val startY = 12.dp.toPx()
                val endX = size.width - 5.dp.toPx()
                val endY = size.height - 24.dp.toPx()
                val route = Path().apply {
                    moveTo(startX, startY)
                    lineTo(size.width * .28f, startY)
                    cubicTo(size.width * .72f, startY, size.width * .3f, endY, size.width * .76f, endY)
                    lineTo(endX, endY)
                }
                drawPath(route, line, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(startColor, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(startX, startY))
                drawCircle(endColor, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(endX, endY))
            }
            TextButton(onClick = onOpenInstagram, interactionSource = interaction,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(start = 8.dp, end = 0.dp)) {
                Text("Open Instagram", style = MaterialTheme.typography.labelLarge, color = Frost)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.NorthEast, null, Modifier.size(17.dp), tint = Frost)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Minor detour", widthDp = 360, showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Minor detour / larger text", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun MinorDetourPreview() {
    DoomlyTheme { Box(Modifier.background(Void).padding(24.dp)) { MinorDetour {} } }
}
