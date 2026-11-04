package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.graphics.graphicsLayer
import java.time.LocalDate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun HomeScreen(viewModel: TodayViewModel = viewModel(), resumeCount: Int = 0) {
    val context = LocalContext.current
    val snapshot by viewModel.getSnapshot().observeAsState()
    val coach by viewModel.getCoachMessage().observeAsState()
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var goalText by rememberSaveable { mutableStateOf("") }

    com.doomly.app.ui.components.StatsRefreshEffect { viewModel.refresh() }

    val reels = snapshot?.todayReels ?: 0
    val total = snapshot?.totalReels ?: 0
    val streak = snapshot?.streak ?: 0
    val target = snapshot?.dailyTarget?.coerceAtLeast(1) ?: DoomStatsStore.DEFAULT_DAILY_TARGET
    val progress = (reels.toFloat() / target).coerceIn(0f, 1f)
    val animatedReels by animateIntAsState(reels, tween(450), label = "reels")
    val animatedProgress by animateFloatAsState(progress, tween(700), label = "progress")

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

    val reelInteraction = remember { MutableInteractionSource() }
    val reelPressed by reelInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (reelPressed) .97f else 1f,
        spring(dampingRatio = .65f, stiffness = 700f), label = "Reels button press")
    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val history = DoomStatsStore.dailyHistory(context)
    Column(Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { TurnHeader("Today") }
            Row(Modifier.background(Mint.copy(alpha = .12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Frost, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("$streak ${if (streak == 1) "day" else "days"}", style = MaterialTheme.typography.titleSmall, color = Frost)
                    Text("STREAK", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        WeekStrip(history, reels)
        Spacer(Modifier.height(24.dp))
        TrackingStatus(resumeCount)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMetric("Reels today", "$animatedReels", "Thumb work logged", Peach,
                Modifier.weight(1f).fillMaxHeight(), scale = countPop.value)
            Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp))
                .background(Aqua).border(1.dp, Hairline.copy(alpha = .45f), RoundedCornerShape(10.dp)).padding(16.dp)) {
                Text("Daily goal", style = MaterialTheme.typography.bodySmall, color = Muted)
                Spacer(Modifier.height(12.dp))
                Text("$target", style = MaterialTheme.typography.headlineLarge, color = Frost)
                TextButton(onClick = { goalText = target.toString(); editGoal = true }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(14.dp), tint = Frost)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit goal", style = MaterialTheme.typography.labelMedium, color = Frost)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeMetric("Next milestone", "${MotivationEngine.nextMilestone(reels, target)}", "Reels", Void,
                Modifier.weight(1f).fillMaxHeight())
            HomeMetric("Your level", "${MotivationEngine.level(total)}", "Professional procrastinator", Violet,
                Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(5.dp),
            color = Mint, trackColor = PanelRaised)
        Spacer(Modifier.height(8.dp))
        Text(if (reels >= target) "Goal crushed. Extremely busy doing nothing." else "${target - reels} Reels to today's goal.",
            style = MaterialTheme.typography.bodySmall, color = Muted)
        MilestonePop(milestone, celebrationEvent)
        goalFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted,
            modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                CloudSyncScheduler.scheduleSync(context)
                (context.findActivity() as? MainActivity)?.openInstagram()
            },
            interactionSource = reelInteraction,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).graphicsLayer {
                scaleX = buttonScale; scaleY = buttonScale
            },
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Frost, contentColor = Void)
        ) {
            Text("One more Reel", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(coach ?: MotivationEngine.message(reels, target, streak),
            style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeMetric(label: String, value: String, caption: String, tint: Color,
                       modifier: Modifier = Modifier, scale: Float = 1f) {
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(tint)
        .border(1.dp, Hairline.copy(alpha = .45f), RoundedCornerShape(10.dp)).padding(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Frost,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
        Spacer(Modifier.height(8.dp))
        Text(caption, style = MaterialTheme.typography.bodySmall, color = Muted)
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
                    "Enter a number from 1 to 20,000." else "A tiny ambition for your very busy thumb.") },
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
