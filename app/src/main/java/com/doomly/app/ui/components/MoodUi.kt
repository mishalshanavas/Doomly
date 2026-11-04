package com.doomly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import java.time.LocalDate

@Composable
fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Void)
            .border(1.dp, Hairline.copy(alpha = .8f), RoundedCornerShape(7.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun PageHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.headlineMedium, color = Frost)
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

/** A compact GitHub-style contribution graph for the last 30 days of Reel use. */
@Composable
fun DailyUsageHeatmap(
    history: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
    val peak = days.maxOf { history[it.toString()].orZero() }.coerceAtLeast(1)

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DAILY USAGE", style = MaterialTheme.typography.labelSmall, color = Muted)
            Text("LAST 30 DAYS", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            days.chunked(15).forEach { fortnight ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    fortnight.forEach { day ->
                        val count = history[day.toString()].orZero()
                        Box(Modifier.weight(1f).aspectRatio(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(usageColor(count, peak))
                            .semantics { contentDescription = "$day: $count Reels" })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.width(6.dp))
            listOf(0f, .25f, .5f, .75f, 1f).forEach { level ->
                Box(
                    Modifier.padding(horizontal = 2.dp).size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (level == 0f) PanelRaised else Mint.copy(alpha = .28f + level * .72f))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }
}

private fun Int?.orZero() = this ?: 0

@Composable
private fun usageColor(count: Int, peak: Int): Color =
    if (count <= 0) PanelRaised else Mint.copy(alpha = (.22f + count.toFloat() / peak * .78f).coerceIn(.22f, 1f))
