package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

    Column(
        Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        PageHeader(
            title = "Dashboard",
            subtitle = "Welcome back. Your thumb has been busy.",
            trailing = { StatusPill("TRACKING") }
        )
        Spacer(Modifier.height(12.dp))

        DottedOrbit(animatedProgress, Modifier.fillMaxWidth().height(190.dp))

        Text("$percent%", style = MaterialTheme.typography.displayMedium, color = Frost)
        Text(
            if (progress >= 1f) "of today’s goal — completed. The algorithm salutes you."
            else "of today’s goal — $animatedReels of $target Reels counted.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted
        )
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardMetric("CURRENT STREAK", if (streak == 1) "1 day" else "$streak days", Modifier.weight(1f))
            DashboardMetric("GOAL REMAINING", "${(target - reels).coerceAtLeast(0)} Reels", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))

        PremiumCard(
            Modifier.fillMaxWidth().semantics { role = Role.Button }.clickable {
                goalText = target.toString()
                editGoal = true
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DAILY LIMIT", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(6.dp))
                    Text("$target Reels", style = MaterialTheme.typography.titleMedium)
                    Text("Tap to edit your definition of ‘just one more.’", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Icon(Icons.Rounded.Edit, "Edit daily goal", tint = Frost, modifier = Modifier.size(19.dp))
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Next milestone", "LEVEL ${MotivationEngine.level(total)}")
        Spacer(Modifier.height(8.dp))
        PremiumCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).background(Frost, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.NorthEast, null, tint = Void, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(milestoneTitle(reels), style = MaterialTheme.typography.titleMedium)
                    Text(milestoneCopy(reels), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text("${reels % 25}/25", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().background(Mint.copy(alpha = .22f), RoundedCornerShape(14.dp)).padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NOTE", style = MaterialTheme.typography.labelSmall, color = Frost)
            Spacer(Modifier.width(12.dp))
            Text(
                coach ?: MotivationEngine.message(reels, target, streak),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                CloudSyncScheduler.scheduleSync(context)
                (context.findActivity() as? MainActivity)?.openInstagram()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Frost, contentColor = Void)
        ) {
            Text(if (progress >= 1f) "Keep scrolling anyway" else "Open Reels", fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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

private fun milestoneTitle(reels: Int) = if (reels > 0 && reels % 25 == 0) "Milestone unlocked" else "The tiny thumb marathon"
private fun milestoneCopy(reels: Int) = if (reels > 0 && reels % 25 == 0) "Twenty-five down. Hydrate the thumb." else "${25 - reels % 25} more to your next ridiculous achievement."
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
