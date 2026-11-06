package com.doomly.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*

@Composable
fun YesterdayGhost(today: Int, yesterday: Int?) {
    val scale = maxOf(today, yesterday ?: 0, 1).toFloat()
    val current by animateFloatAsState(today.coerceAtLeast(0) / scale, tween(350), label = "Today's comparison marker")
    val track = Hairline
    val ghost = Muted.copy(alpha = .55f)
    val accent = Mint
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Yesterday’s ghost", style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(yesterday?.let { "$it Reels" } ?: "Available tomorrow", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        if (yesterday != null) Canvas(Modifier.fillMaxWidth().height(26.dp).semantics {
            contentDescription = "Today: $today Reels. Yesterday: $yesterday Reels. Dot is today; faint line is yesterday."
        }) {
            val inset = 4.dp.toPx()
            val length = (size.width - inset * 2).coerceAtLeast(0f)
            val y = size.height / 2
            drawLine(track, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx())
            val x = inset + length * (yesterday.coerceAtLeast(0) / scale)
            drawLine(ghost, Offset(x, y - 5.dp.toPx()), Offset(x, y + 5.dp.toPx()), 1.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(accent, 3.dp.toPx(), Offset(inset + length * current, y))
        }
    }
}
