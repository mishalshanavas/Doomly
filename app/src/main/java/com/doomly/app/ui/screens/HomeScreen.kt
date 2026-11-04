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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun HomeScreen(viewModel: TodayViewModel = viewModel()) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val snapshot by viewModel.getSnapshot().observeAsState()
    val coach by viewModel.getCoachMessage().observeAsState()
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalText by rememberSaveable { mutableStateOf("") }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

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
            if (reels / 25 > previous / 25) {
                milestone = reels / 25 * 25
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
            CloudSyncScheduler.updateDailyTarget(context, goalText.toIntOrNull()?.coerceIn(1, 20_000) ?: target, quietCallback())
            viewModel.refresh()
            editGoal = false
        },
        onDismiss = { editGoal = false }
    )

    val reelInteraction = remember { MutableInteractionSource() }
    val reelPressed by reelInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (reelPressed) .97f else 1f,
        spring(dampingRatio = .65f, stiffness = 700f), label = "Reels button press")
    val history = DoomStatsStore.dailyHistory(context)
    Column(Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        TurnHeader("It's your turn")
        Spacer(Modifier.height(28.dp))
        PremiumCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("TODAY'S REELS", style = MaterialTheme.typography.labelSmall, color = Muted)
                PlayfulPawn()
            }
            Text("$animatedReels", style = MaterialTheme.typography.displayLarge, color = Frost,
                modifier = Modifier.graphicsLayer { scaleX = countPop.value; scaleY = countPop.value })
            Spacer(Modifier.height(8.dp))
            Text("Daily goal: $target", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (progress >= 1f) Frost else Mint, trackColor = PanelRaised)
            Spacer(Modifier.height(12.dp))
            Text(if (reels >= target) "Goal crushed. Thumb athlete status: confirmed." else "${target - reels} to today's goal.",
                style = MaterialTheme.typography.bodyMedium, color = Frost)
            MilestonePop(milestone, celebrationEvent)
        }
        TextButton(onClick = { goalText = target.toString(); editGoal = true }) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = Muted)
            Spacer(Modifier.width(8.dp))
            Text("Adjust daily goal", color = Muted)
        }
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
        Spacer(Modifier.height(26.dp))
        DisclosureSection("More details", "Your week, streak and milestones") {
            WeekStrip(history, reels)
            Spacer(Modifier.height(18.dp))
            BoardRow {
                TurnTile("Current\nstreak", "$streak days", com.doomly.app.R.drawable.board_tree,
                    Modifier.weight(1f).fillMaxHeight())
                TileDivider()
                TurnTile("Your\nlevel", "${MotivationEngine.level(total)}", com.doomly.app.R.drawable.board_trophy,
                    Modifier.weight(1f).fillMaxHeight(), Peach)
                TileDivider()
                TurnTile("Next\nmilestone", "${reels % 25}/25", com.doomly.app.R.drawable.board_flag,
                    Modifier.weight(1f).fillMaxHeight())
            }
        }
        Spacer(Modifier.height(24.dp))
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

private fun quietCallback() = object : ResultCallback<Void> {
    override fun onSuccess(value: Void?) = Unit
    override fun onError(message: String) = Unit
}

private fun Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}
