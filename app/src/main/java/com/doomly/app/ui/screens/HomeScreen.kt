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
                override fun onError(message: String) { goalFeedback = "Target saved on this device. Cloud sync will retry later."; CloudSyncScheduler.scheduleSync(context) }
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
        PageHeader("Today")
        Spacer(Modifier.height(16.dp))
        Text("Instagram Reels", style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(20.dp))
        TrackingStatus(resumeCount)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().background(Panel, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(24.dp)) {
            Text("TODAY’S TALLY", style = MaterialTheme.typography.labelSmall, color = Muted)
            Spacer(Modifier.height(12.dp))
            Text(if (snapshot == null) "—" else "$animatedReels", style = MaterialTheme.typography.displayLarge, color = Frost,
                modifier = Modifier.graphicsLayer { scaleX = countPop.value; scaleY = countPop.value })
            Text(if (snapshot == null) "Loading your daily tally…" else "Reels", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(progress = { (reels.toFloat() / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp), color = Mint, trackColor = PanelRaised)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Target · $target${if (reels >= target) " · reached" else ""}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Muted)
                TextButton(onClick = { goalText = target.toString(); editGoal = true }) { Text("Adjust goal") }
            }
        }
        YesterdayGhost(today = reels, yesterday = history[LocalDate.now().minusDays(1).toString()])


        goalFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) }
        Spacer(Modifier.height(20.dp))

        Spacer(Modifier.height(24.dp))
        Text("This week", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        WeekStrip(history, reels)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "My Doomly daily score: $reels Instagram Reels. ${ScrollLevel.forCount(reels).title}. A very busy thumb.")
            }
            try { context.startActivity(android.content.Intent.createChooser(share, "Share your score")) }
            catch (_: android.content.ActivityNotFoundException) { goalFeedback = "No sharing app is available." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Share today's score") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalDialog(value: String, onValue: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set your daily target") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { onValue(it.filter(Char::isDigit).take(5)) },
                label = { Text("Reels per day") },
                supportingText = { Text(if (value.isNotEmpty() && value.toIntOrNull() !in 1..20_000)
                    "Enter a number from 1 to 20,000." else "A score to aim for. Tracking continues past your target.") },
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
