package com.doomly.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TurnHeader(title: String, subtitle: String? = null) {
    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
        style = MaterialTheme.typography.labelSmall, color = Frost)
    Spacer(Modifier.height(12.dp))
    PageHeader(title, subtitle)
}

/** Each marker shows recorded usage, never inferred completion or an invented streak. */
@Composable
fun WeekStrip(history: Map<String, Int>, todayCount: Int) {
    val today = LocalDate.now()
    val first = today.minusDays((today.dayOfWeek.value % 7).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(7) { index ->
            val date = first.plusDays(index.toLong())
            val current = date == today
            val count = if (current) todayCount else history[date.toString()] ?: 0
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { contentDescription = "$date: $count Reels${if (current) ", today" else ""}" }) {
                Text(if (current) "Today" else date.format(DateTimeFormatter.ofPattern("EEEEE")),
                    style = MaterialTheme.typography.labelSmall, color = if (current) Frost else Muted)
                Spacer(Modifier.height(7.dp))
                Box(Modifier.size(34.dp).clip(CircleShape).background(if (current) Frost else Color.Transparent), Alignment.Center) {
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium,
                        color = if (current) Void else if (date <= today) Frost else Muted)
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(3.dp).clip(CircleShape).background(if (count > 0) Mint else Color.Transparent))
            }
        }
    }
}

@Composable
fun BoardArt(@DrawableRes asset: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(asset), description, modifier)
}
