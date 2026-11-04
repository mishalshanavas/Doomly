package com.doomly.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TurnHeader(title: String, subtitle: String? = null) {
    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())).uppercase(),
        style = MaterialTheme.typography.labelSmall, color = Frost)
    Spacer(Modifier.height(5.dp))
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
                Box(Modifier.size(38.dp).border(if (current) 1.5.dp else 0.dp, if (current) Frost else Color.Transparent, CircleShape).padding(3.dp), Alignment.Center) {
                    Box(Modifier.fillMaxSize().clip(CircleShape)
                        .background(if (count > 0) Mint else Void)
                        .border(1.dp, if (date <= today) Frost else PanelRaised, CircleShape), Alignment.Center) {
                        Text("${date.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, color = Frost)
                    }
                }
            }
        }
    }
}

@Composable
fun BoardArt(@DrawableRes asset: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(asset), description, modifier)
}

@Composable
fun TurnTile(title: String, value: String, @DrawableRes asset: Int,
             modifier: Modifier = Modifier, tint: Color = Color.Transparent,
             onClick: (() -> Unit)? = null) {
    Column(modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
        .background(tint).padding(top = 17.dp, start = 6.dp, end = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Frost)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Frost)
        Spacer(Modifier.weight(1f))
        BoardArt(asset, Modifier.size(51.dp))
    }
}

@Composable
fun BoardConnector(toRight: Boolean) {
    Row(Modifier.fillMaxWidth()) {
        if (toRight) Spacer(Modifier.weight(2f))
        Box(Modifier.weight(1f).height(24.dp).border(1.dp, Hairline), Alignment.Center) {
            Text("⌄", color = Frost, style = MaterialTheme.typography.bodyMedium)
        }
        if (!toRight) Spacer(Modifier.weight(2f))
    }
}

@Composable
fun BoardRow(content: @Composable RowScope.() -> Unit) {
    val tileHeight = (142 * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp
    Row(Modifier.fillMaxWidth().height(tileHeight).clip(RoundedCornerShape(7.dp))
        .border(1.dp, Hairline, RoundedCornerShape(7.dp)), content = content)
}

@Composable
fun TileDivider() { Box(Modifier.width(1.dp).fillMaxHeight().background(Hairline)) }

@androidx.compose.ui.tooling.preview.Preview(name = "Board / 360dp", widthDp = 360, showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Board / larger text", widthDp = 320, fontScale = 1.5f, showBackground = true)
@Composable
private fun TurnBoardPreview() {
    DoomlyTheme {
        Column(Modifier.background(Void).padding(20.dp)) {
            TurnHeader("It's your turn")
            Spacer(Modifier.height(20.dp))
            WeekStrip(emptyMap(), 12)
            Spacer(Modifier.height(20.dp))
            BoardRow {
                TurnTile("Today's\nReels", "12", com.doomly.app.R.drawable.board_phone, Modifier.weight(1f).fillMaxHeight(), Peach)
                TileDivider()
                TurnTile("Daily\nlimit", "100", com.doomly.app.R.drawable.board_flag, Modifier.weight(1f).fillMaxHeight())
                TileDivider()
                TurnTile("Limit\nused", "12%", com.doomly.app.R.drawable.board_clock, Modifier.weight(1f).fillMaxHeight())
            }
            BoardConnector(true)
            BoardRow {
                TurnTile("Current\nstreak", "3 days", com.doomly.app.R.drawable.board_tree, Modifier.weight(1f).fillMaxHeight())
                TileDivider()
                TurnTile("Remaining\ntoday", "88", com.doomly.app.R.drawable.board_coffee, Modifier.weight(1f).fillMaxHeight(), Aqua)
                TileDivider()
                TurnTile("Your\nlevel", "2", com.doomly.app.R.drawable.board_trophy, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
