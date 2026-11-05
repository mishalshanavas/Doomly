package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.graphics.graphicsLayer
import java.time.LocalDate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun HomeScreen(viewModel: TodayViewModel = viewModel(), resumeCount: Int = 0) {
    val context = LocalContext.current
    val snapshot by viewModel.getSnapshot().observeAsState()
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var goalText by rememberSaveable { mutableStateOf("") }

    com.doomly.app.ui.components.StatsRefreshEffect { viewModel.refresh() }

    val reels = snapshot?.todayReels ?: 0
    val streak = snapshot?.streak ?: 0
    val target = snapshot?.dailyTarget?.coerceAtLeast(1) ?: DoomStatsStore.DEFAULT_DAILY_TARGET
    val animatedReels by animateIntAsState(reels, tween(450), label = "reels")

    val dayKey = LocalDate.now().toString()
    var lastCount by rememberSaveable(dayKey) { mutableStateOf<Int?>(null) }
    var milestone by rememberSaveable(dayKey) { mutableIntStateOf(0) }
    var celebrationEvent by rememberSaveable(dayKey) { mutableIntStateOf(0) }
    val countPop = remember { Animatable(1f) }
    LaunchedEffect(snapshot?.todayReels, dayKey) {
        if (snapshot == null) return@LaunchedEffect
        val previous = lastCount
        lastCount = reels
        // Hydration establishes a baseline; returning with new counts can celebrate once.
        if (previous != null && reels > previous) {
            if (MotivationEngine.reachedMilestone(reels, target) > MotivationEngine.reachedMilestone(previous, target)) {
                milestone = MotivationEngine.reachedMilestone(reels, target)
                celebrationEvent++
            }
            countPop.snapTo(1f)
            countPop.animateTo(1f, keyframes {
                durationMillis = 300
                1.06f at 100
                1f at 300
            })
        }
    }

    if (editGoal) GoalDialog(
        value = goalText,
        onValue = { goalText = it },
        onSave = {
            goalFeedback = "Daily goal updated."
            CloudSyncScheduler.updateDailyTarget(context, goalText.toIntOrNull()?.coerceIn(1, 20_000) ?: target, object : ResultCallback<Void> {
                override fun onSuccess(value: Void?) = Unit
                override fun onError(message: String) = Unit
            })
            viewModel.refresh()
            editGoal = false
        },
        onDismiss = { editGoal = false }
    )

    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val history = DoomStatsStore.dailyHistory(context)
    Column(Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { TurnHeader("Today") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Muted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("$streak-day streak", style = MaterialTheme.typography.bodySmall, color = Frost)
            }
        }
        Spacer(Modifier.height(24.dp))
        WeekStrip(history, reels)
        Spacer(Modifier.height(24.dp))
        TrackingStatus(resumeCount)
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            HomeMetric("Reels today", "$animatedReels", Modifier.weight(1f).fillMaxHeight(),
                scale = countPop.value, primary = true)
            Box(Modifier.fillMaxHeight().width(1.dp).background(Hairline))
            Column(Modifier.weight(1f).padding(start = 24.dp, top = 22.dp, bottom = 12.dp)) {
                Text("Daily goal", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(12.dp))
                Text("$target", style = MaterialTheme.typography.headlineLarge, color = Frost)
                TextButton(onClick = { goalText = target.toString(); editGoal = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("Edit", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
            }
        }
        HorizontalDivider(color = Hairline)
        YesterdayGhost(today = reels, yesterday = history[LocalDate.now().minusDays(1).toString()])
        if (snapshot != null) DailyAchievementBadge(reels = reels, dailyGoal = target)
        MilestonePop(milestone, celebrationEvent)
        goalFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) }
        MinorDetour(onOpenInstagram = {
            CloudSyncScheduler.scheduleSync(context)
            (context.findActivity() as? MainActivity)?.openInstagram()
        })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeMetric(label: String, value: String, modifier: Modifier = Modifier,
                       scale: Float = 1f, primary: Boolean = false) {
    Column(modifier.padding(top = 22.dp, bottom = 22.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(12.dp))
        Text(value, style = if (primary) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
            color = Frost, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
    }
}

@Composable
private fun GoalDialog(value: String, onValue: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a daily goal") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { onValue(it.filter(Char::isDigit).take(5)) },
                label = { Text("Reels per day") },
                supportingText = { Text(if (value.isNotEmpty() && value.toIntOrNull() !in 1..20_000)
                    "Enter a number from 1 to 20,000." else "You can change this anytime.") },
                isError = value.isNotEmpty() && value.toIntOrNull() !in 1..20_000,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = { Button(onClick = onSave, enabled = value.toIntOrNull() in 1..20_000) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}
