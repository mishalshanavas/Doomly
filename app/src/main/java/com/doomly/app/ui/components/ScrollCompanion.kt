package com.doomly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.doomly.app.ScrollLevel
import com.doomly.app.ui.theme.*

/** Original vector companion, deliberately independent of a health metaphor. */
@Composable
fun ScrollCompanion(reels: Int, modifier: Modifier = Modifier) {
    val level = ScrollLevel.forCount(reels)
    val ink = Frost
    val paper = Void
    val accent = Mint
    Column(modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(12.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(76.dp)) {
                val w = size.width
                drawCircle(accent.copy(alpha = .18f), w * .48f)
                drawOval(ink, Offset(w * .12f, w * .31f), Size(w * .32f, w * .4f))
                drawOval(ink, Offset(w * .52f, w * .31f), Size(w * .32f, w * .4f))
                drawCircle(paper, w * .055f, Offset(w * .34f, w * .5f))
                drawCircle(paper, w * .055f, Offset(w * .74f, w * .5f))
                if (level.index >= 2) {
                    val crown = Path().apply {
                        moveTo(w * .26f, w * .27f); lineTo(w * .2f, w * .08f)
                        lineTo(w * .4f, w * .17f); lineTo(w * .5f, 0f)
                        lineTo(w * .6f, w * .17f); lineTo(w * .8f, w * .08f)
                        lineTo(w * .74f, w * .27f); close()
                    }
                    drawPath(crown, accent)
                }
                drawArc(ink, 15f, 150f, false, Offset(w * .38f, w * .67f), Size(w * .23f, w * .16f), style = Stroke(w * .025f))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("MOCHI · LEVEL ${level.index + 1}", style = MaterialTheme.typography.labelSmall, color = Muted)
                Text(level.title, style = MaterialTheme.typography.titleLarge, color = Frost)
                Spacer(Modifier.height(4.dp))
                Text(if (level.next == 0) "Maximum level reached" else "${level.next - reels.coerceAtLeast(0)} Reels to the next look.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { level.progress }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = PanelRaised)
        Spacer(Modifier.height(8.dp))
        Text("Resets daily", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
