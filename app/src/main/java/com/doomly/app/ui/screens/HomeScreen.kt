package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.MoodFace
import com.doomly.app.ui.components.SoftCard
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
        owner.lifecycle.addObserver(observer); viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val reels = snapshot?.todayReels ?: 0
    val target = snapshot?.dailyTarget?.coerceAtLeast(1) ?: DoomStatsStore.DEFAULT_DAILY_TARGET
    val progress = (reels.toFloat() / target).coerceIn(0f, 1f)
    val animatedReels by animateIntAsState(reels, tween(550), label = "reels")
    val animatedProgress by animateFloatAsState(progress, tween(700), label = "progress")
    val history = DoomStatsStore.dailyHistory(context)

    if (editGoal) AlertDialog(
        onDismissRequest = { editGoal = false },
        title = { Text("Choose your daily goal") },
        text = { OutlinedTextField(goalText, { goalText = it.filter(Char::isDigit).take(5) }, label = { Text("Reels per day") }, singleLine = true) },
        confirmButton = { Button(onClick = {
            CloudSyncScheduler.updateDailyTarget(context, goalText.toIntOrNull()?.coerceIn(1, 20_000) ?: target, quietCallback())
            viewModel.refresh(); editGoal = false
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { editGoal = false }) { Text("Cancel") } }
    )

    Column(Modifier.fillMaxSize().background(Sunshine).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = MaterialTheme.typography.labelMedium, color = Ink.copy(alpha = .58f))
            Text("Today", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(22.dp))
            MoodFace(animatedProgress, Modifier.width(180.dp))
            Text(moodLabel(progress), style = MaterialTheme.typography.headlineLarge)
            Text("$animatedReels reels", style = MaterialTheme.typography.titleMedium, color = Ink.copy(alpha = .62f))
            Spacer(Modifier.height(12.dp))
            Text(
                coach ?: MotivationEngine.message(reels, target, snapshot?.streak ?: 0),
                style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = .72f),
                textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 310.dp)
            )
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .background(Paper).padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            SoftCard(Modifier.fillMaxWidth().clickable { goalText = target.toString(); editGoal = true }) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Daily journey", style = MaterialTheme.typography.titleMedium)
                        Text("${MotivationEngine.remaining(reels, target)} reels to go", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = Coral)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = Coral, trackColor = SunshineSoft
                )
                Spacer(Modifier.height(10.dp))
                Text("Tap to change your $target Reel goal", style = MaterialTheme.typography.labelSmall, color = Muted)
            }

            Spacer(Modifier.height(18.dp))
            WeekStrip(history, reels)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { CloudSyncScheduler.scheduleSync(context); (context.findActivity() as? MainActivity)?.openInstagram() },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White)
            ) { Text(if (progress >= 1f) "Beat today's best" else "Open Instagram") }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("🔥", "${snapshot?.streak ?: 0}", "day streak", Modifier.weight(1f))
                MiniStat("✨", "${MotivationEngine.level(snapshot?.totalReels ?: 0)}", "level", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WeekStrip(history: Map<String, Int>, todayReels: Int) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            Text("Keep your rhythm", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }.forEach { day ->
                val count = if (day == LocalDate.now()) todayReels else history[day.toString()] ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.format(DateTimeFormatter.ofPattern("E")).take(1), style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(if (count > 0) Sunshine else Color(0xFFF2EEE6)), Alignment.Center) {
                        Text(if (count > 0) "•" else "–", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: String, value: String, label: String, modifier: Modifier) {
    SoftCard(modifier) {
        Text(icon, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

private fun moodLabel(progress: Float) = when {
    progress >= 1f -> "Unstoppable"; progress >= .65f -> "Happy"
    progress >= .25f -> "Warming up"; else -> "Ready"
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
