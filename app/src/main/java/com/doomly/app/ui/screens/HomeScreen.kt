package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import kotlin.math.roundToInt

@Composable
fun HomeScreen(viewModel: TodayViewModel = viewModel()) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val snapshot by viewModel.getSnapshot().observeAsState()
    val coach by viewModel.getCoachMessage().observeAsState()
    var editGoal by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf("") }

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
    val percent = (animatedProgress * 100).roundToInt()

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

    val history = DoomStatsStore.dailyHistory(context)
    Column(Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(26.dp))
        TurnHeader("It's your turn")
        Spacer(Modifier.height(22.dp))
        WeekStrip(history, reels)
        Spacer(Modifier.height(25.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("▢  $animatedReels / $target Reels", style = MaterialTheme.typography.bodyMedium, color = Frost)
            TextButton(onClick = { goalText = target.toString(); editGoal = true }) {
                Icon(Icons.Rounded.Edit, "Edit daily limit", Modifier.size(17.dp), tint = Frost)
                Spacer(Modifier.width(6.dp))
                Text("Daily limit", color = Frost, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        BoardRow {
            TurnTile("Today's\nReels", "$animatedReels", com.doomly.app.R.drawable.board_phone,
                Modifier.weight(1f).fillMaxHeight(), Peach)
            TileDivider()
            TurnTile("Daily\nlimit", "$target", com.doomly.app.R.drawable.board_flag,
                Modifier.weight(1f).fillMaxHeight(), onClick = { goalText = target.toString(); editGoal = true })
            TileDivider()
            TurnTile("Limit\nused", "$percent%", com.doomly.app.R.drawable.board_clock,
                Modifier.weight(1f).fillMaxHeight())
        }
        BoardConnector(true)
        BoardRow {
            TurnTile("Current\nstreak", "$streak days", com.doomly.app.R.drawable.board_tree,
                Modifier.weight(1f).fillMaxHeight())
            TileDivider()
            TurnTile("Remaining\ntoday", "${(target - reels).coerceAtLeast(0)}", com.doomly.app.R.drawable.board_coffee,
                Modifier.weight(1f).fillMaxHeight(), Aqua)
            TileDivider()
            TurnTile("Your\nlevel", "${MotivationEngine.level(total)}", com.doomly.app.R.drawable.board_trophy,
                Modifier.weight(1f).fillMaxHeight())
        }
        BoardConnector(false)
        BoardRow {
            TurnTile("Lifetime\nReels", "$total", com.doomly.app.R.drawable.board_pawn,
                Modifier.weight(1f).fillMaxHeight(), Violet)
            TileDivider()
            TurnTile("Next\nmilestone", "${reels % 25}/25", com.doomly.app.R.drawable.board_trophy,
                Modifier.weight(1f).fillMaxHeight())
            TileDivider()
            TurnTile("Take a\nbreather", "You got this", com.doomly.app.R.drawable.board_coffee,
                Modifier.weight(1f).fillMaxHeight(), Peach)
        }
        Spacer(Modifier.height(14.dp))
        Text(coach ?: MotivationEngine.message(reels, target, streak),
            style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                CloudSyncScheduler.scheduleSync(context)
                (context.findActivity() as? MainActivity)?.openInstagram()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(7.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Frost, contentColor = Void)
        ) {
            Text(if (progress >= 1f) "Keep scrolling anyway" else "Open Reels", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalDialog(value: String, onValue: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a daily limit") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { onValue(it.filter(Char::isDigit).take(5)) },
                label = { Text("Reels per day") },
                supportingText = { Text("Ambitious, but make it survivable.") },
                singleLine = true
            )
        },
        confirmButton = { Button(onClick = onSave, enabled = value.toIntOrNull() != null) { Text("Save") } },
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
