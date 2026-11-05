package com.doomly.app.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doomly.app.DailyAchievement
import com.doomly.app.ui.theme.*
import java.time.LocalDate

/** A compact daily badge; the target and earned state survive process recreation. */
@Composable
fun DailyAchievementBadge(reels: Int, dailyGoal: Int, day: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val dayKey = day.toString()
    val prefs = remember(context) { context.getSharedPreferences("doomly_daily_achievement", Context.MODE_PRIVATE) }
    // Freeze today's target once chosen, so editing the main goal cannot move the badge.
    val achievement = remember(dayKey) {
        val sameDay = prefs.getString("day", "") == dayKey
        val chosen = DailyAchievement.forDay(day, if (sameDay) prefs.getInt("goal", dailyGoal) else dailyGoal)
        if (!sameDay) prefs.edit().putString("day", dayKey).putInt("goal", dailyGoal).putBoolean("earned", false).apply()
        chosen
    }
    var awarded by remember(dayKey) { mutableStateOf(prefs.getBoolean("earned", false)) }
    val complete = awarded || achievement.earned(reels)
    val pop = remember(dayKey) { Animatable(1f) }
    val tint by animateColorAsState(if (complete) Mint else Muted, label = "Daily achievement color")
    LaunchedEffect(dayKey, complete) {
        if (!complete || awarded) return@LaunchedEffect
        prefs.edit().putBoolean("earned", true).apply()
        awarded = true
        pop.animateTo(1f, keyframes { durationMillis = 450; 1.16f at 150; 1f at 450 })
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(if (complete) Mint.copy(alpha = .08f) else Panel, CircleShape), Alignment.Center) {
            Icon(if (complete) Icons.Outlined.CheckCircle else Icons.Outlined.WorkspacePremium, null,
                Modifier.size(20.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Daily achievement", style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(3.dp))
            Text(achievement.title, style = MaterialTheme.typography.titleSmall, color = Frost)
            Spacer(Modifier.height(3.dp))
            Text(if (complete) "Earned today · ${achievement.target} Reels" else "Watch ${achievement.target} Reels today",
                style = MaterialTheme.typography.bodySmall, color = Muted,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.width(8.dp))
        Text(if (complete) "✓" else "${achievement.progress(reels)}/${achievement.target}",
            style = MaterialTheme.typography.labelMedium, color = if (complete) Frost else Muted)
    }
}
