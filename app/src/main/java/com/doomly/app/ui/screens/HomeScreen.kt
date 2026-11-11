package com.doomly.app.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: TodayViewModel = viewModel(), resumeCount: Int = 0) {
    val context = LocalContext.current
    val reducedMotion = LocalReduceMotion.current
    val snapshot by viewModel.getSnapshot().observeAsState()
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var shareRecap by remember { mutableStateOf<DailyRecap?>(null) }
    shareRecap?.let { DailyRecapDialog(it) { shareRecap = null } }
    StatsRefreshEffect { viewModel.refresh() }
    val reels = snapshot?.todayReels ?: 0
    val streak = snapshot?.streak ?: 0
    val target = snapshot?.dailyTarget?.coerceAtLeast(1) ?: DoomStatsStore.DEFAULT_DAILY_TARGET
    val animatedReels by animateIntAsState(reels, tween(motionDuration(450)), label = "reels")
    val reached = reels >= target
    val level = ScrollLevel.forCount(reels)
    val next = MotivationEngine.nextMilestone(reels, target)
    val dayKey = LocalDate.now().toString()
    var lastCount by rememberSaveable(dayKey) { mutableStateOf<Int?>(null) }
    var milestone by rememberSaveable(dayKey) { mutableIntStateOf(0) }
    var celebrationEvent by rememberSaveable(dayKey) { mutableIntStateOf(0) }
    val countPop = remember { Animatable(1f) }
    LaunchedEffect(snapshot?.todayReels, dayKey, reducedMotion) {
        countPop.snapTo(1f)
        if (snapshot == null) return@LaunchedEffect
        val previous = lastCount; lastCount = reels
        if (previous != null && reels > previous) {
            if (MotivationEngine.reachedMilestone(reels, target) > MotivationEngine.reachedMilestone(previous, target)) {
                milestone = MotivationEngine.reachedMilestone(reels, target); celebrationEvent++
            }
            if (!reducedMotion) {
                countPop.snapTo(1f); countPop.animateTo(1f, keyframes { durationMillis = 300; 1.04f at 100; 1f at 300 })
            }
        }
    }
    if (editGoal) GoalDialog(goalText, { goalText = it }, {
        goalFeedback = "Goal updated"
        CloudSyncScheduler.updateDailyTarget(context, goalText.toIntOrNull()?.coerceIn(1, 20_000) ?: target, object : ResultCallback<Void> {
            override fun onSuccess(value: Void?) = Unit
            override fun onError(message: String) { goalFeedback = "Saved on device"; CloudSyncScheduler.scheduleSync(context) }
        }); viewModel.refresh(); editGoal = false
    }, { editGoal = false })
    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val history = DoomStatsStore.dailyHistory(context)
    val todayLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

    Column(Modifier.fillMaxSize().background(BodyBg).verticalScroll(rememberScrollState())) {
        TrackingStatus(resumeCount, Modifier.statusBarsPadding().padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg))
        TopSheet {
            SheetContent {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reels today", style = MaterialTheme.typography.titleLarge.regular, color = Smoke, modifier = Modifier.weight(1f))
                    Text(todayLabel, style = MaterialTheme.typography.bodyMedium, color = Smoke)
                }
                if (snapshot == null) CounterSkeleton()
                else Text(
                    animatedReels.grouped(),
                    style = tabular(MaterialTheme.typography.displayLarge), color = Ink, maxLines = 1,
                    modifier = Modifier.graphicsLayer { scaleX = countPop.value; scaleY = countPop.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                )
                Text(
                    "of ${target.grouped()}  ·  ${level.title}" + if (streak > 0) "  ·  $streak-day streak" else "",
                    style = tabular(MaterialTheme.typography.bodyMedium), color = Smoke
                )
                Spacer(Modifier.height(Spacing.xxl))
                DottedProgress(reels.toFloat() / target, complete = reached, description = "$reels of $target Reels")
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (reached) "Goal reached" else "${(target - reels).grouped()} reels to go",
                        style = tabular(MaterialTheme.typography.bodySmall), color = Smoke,
                        modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    Text(
                        "Edit goal", style = MaterialTheme.typography.bodySmall, color = Ink,
                        modifier = Modifier.clip(CircleShape).clickable { goalText = target.toString(); editGoal = true }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.md)
                    )
                }
                MilestoneNotice(milestone, celebrationEvent)
                goalFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Smoke) }
            }
            Spacer(Modifier.height(Spacing.lg))
            InsetCard {
                LabelStack(
                    label = "Up next…",
                    title = "${next.grouped()} reels",
                    meta = "${(next - reels).grouped()} more to the next milestone"
                )
            }
        }

        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Spacer(Modifier.height(Spacing.lg))
            TextTabs(listOf("Week", "Month", "Levels"), tab, { tab = it })
            Spacer(Modifier.height(Spacing.lg))
            AnimatedContent(tab, transitionSpec = {
                if (reducedMotion) (EnterTransition.None togetherWith ExitTransition.None).using(null)
                else fadeIn(tween(180)) togetherWith fadeOut(tween(120))
            }, label = "tab") { selected ->
                when (selected) {
                    0 -> Column {
                        SectionHeading("Reels per day")
                        GroupedSection { WeekChart(history, reels) }
                    }
                    1 -> Column {
                        SectionHeading("Last 30 days")
                        GroupedSection { DotMatrix(history + (dayKey to reels)) }
                    }
                    else -> LevelList(reels)
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
            PrimaryButton("Open Instagram", onClick = { (context.findActivity() as? MainActivity)?.openInstagram() })
            QuietButton("Share today's count", onClick = {
                val current = DoomStatsStore.snapshot(context)
                shareRecap = DailyRecap(LocalDate.now(), current.todayReels, current.dailyTarget, current.streak, DoomStatsStore.dailyHistory(context))
            })
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/** Levels as a checklist: upcoming first, reached ones ticked and struck through. */
@Composable
private fun LevelList(reels: Int) {
    val current = ScrollLevel.forCount(reels).index
    Column {
        if (current + 1 < ScrollLevel.levels()) {
            SectionHeading("Up next")
            for (i in current + 1 until ScrollLevel.levels()) {
                CheckRow(ScrollLevel.name(i), done = false, meta = "${(ScrollLevel.threshold(i) - reels).grouped()} more reels")
                Spacer(Modifier.height(Spacing.sm))
            }
            Spacer(Modifier.height(Spacing.lg))
        }
        SectionHeading("Reached")
        for (i in current downTo 0) {
            CheckRow(ScrollLevel.name(i), done = true)
            if (i > 0) Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun MilestoneNotice(milestone: Int, event: Int) {
    val reduced = LocalReduceMotion.current
    var visible by remember { mutableStateOf(false) }
    var handledEvent by rememberSaveable { mutableIntStateOf(event) }
    LaunchedEffect(event) {
        if (event == handledEvent) return@LaunchedEffect
        handledEvent = event; if (event == 0) { visible = false; return@LaunchedEffect }
        visible = true; delay(2_400); visible = false
    }
    AnimatedVisibility(visible, enter = if (reduced) EnterTransition.None else fadeIn() + expandVertically(),
        exit = if (reduced) ExitTransition.None else fadeOut() + shrinkVertically()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
            Spacer(Modifier.width(Spacing.sm))
            Text("${milestone.grouped()} reels reached", style = tabular(MaterialTheme.typography.bodySmall), color = Ink)
        }
    }
}

@Composable
internal fun GoalDialog(value: String, onValue: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    val valid = value.toIntOrNull() in 1..20_000
    AlertDialog(onDismissRequest = onDismiss, shape = DoomlyShapes.extraLarge, containerColor = Sheet,
        title = { Text("Daily goal", style = MaterialTheme.typography.headlineSmall, color = Ink) },
        text = {
            Column {
                OutlinedTextField(value, { onValue(it.filter(Char::isDigit).take(5)) }, label = { Text("Reels per day") },
                    isError = value.isNotEmpty() && !valid,
                    supportingText = { if (value.isNotEmpty() && !valid) Text("1–20,000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                    shape = DoomlyShapes.medium, colors = sheetFieldColors())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(100, 200, 500).forEach { preset ->
                        TextButton(onClick = { onValue(preset.toString()) }) { Text(preset.toString(), color = Ink) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = valid) { Text("Save", color = if (valid) Ink else Smoke) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Smoke) } })
}

private fun Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is ContextWrapper) { if (current is android.app.Activity) return current; current = current.baseContext }
    return null
}
