package com.doomly.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.BuildConfig
import com.doomly.app.DoomStatsStore
import com.doomly.app.ProfileViewModel
import com.doomly.app.ScrollLevel
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*
import com.doomly.app.update.AppUpdateViewModel
import com.doomly.app.update.GitHubRelease
import com.doomly.app.update.UpdateState

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onSignOut: () -> Unit = {}, resumeCount: Int = 0, updates: AppUpdateViewModel? = null, onInstallUpdate: () -> Unit = {}) {
    LaunchedEffect(resumeCount) { viewModel.refresh() }
    val context = LocalContext.current
    val name by viewModel.getDisplayName().observeAsState("Doomly user")
    val email by viewModel.getEmail().observeAsState("Local profile")
    val streak by viewModel.getStreak().observeAsState("0")
    val target by viewModel.getDailyTarget().observeAsState(DoomStatsStore.DEFAULT_DAILY_TARGET)
    val total by viewModel.getTotalReels().observeAsState("0")
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    var confirmSignOut by remember { mutableStateOf(false) }
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var goalText by rememberSaveable { mutableStateOf("") }
    var goalFeedback by remember { mutableStateOf<String?>(null) }
    val updateState = updates?.state ?: UpdateState.UpToDate
    StatsRefreshEffect { viewModel.refresh() }
    val todayReels = DoomStatsStore.snapshot(context).todayReels
    val history = DoomStatsStore.dailyHistory(context)
    val best = maxOf(history.values.maxOrNull() ?: 0, todayReels)
    val usageHistory = history + (java.time.LocalDate.now().toString() to todayReels)
    val totalNumber = total.toIntOrNull()

    if (editGoal) GoalDialog(goalText, { goalText = it }, {
        goalFeedback = "Goal updated"
        com.doomly.app.CloudSyncScheduler.updateDailyTarget(context, goalText.toIntOrNull() ?: target, object : com.doomly.app.ResultCallback<Void> {
            override fun onSuccess(value: Void?) = Unit
            override fun onError(message: String) {
                goalFeedback = "Saved on device"
                com.doomly.app.CloudSyncScheduler.scheduleSync(context)
            }
        })
        viewModel.refresh()
        editGoal = false
    }, { editGoal = false })

    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false }, shape = DoomlyShapes.extraLarge, containerColor = Sheet,
        title = { Text("Sign out?", style = MaterialTheme.typography.headlineSmall, color = Ink) },
        text = { Text("Counts stay on this device.", style = MaterialTheme.typography.bodyMedium, color = Smoke) },
        confirmButton = { TextButton(onClick = { AuthRepository.getInstance().signOut(context); viewModel.refresh(); confirmSignOut = false; onSignOut() }) { Text("Sign out", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel", color = Smoke) } })

    Column(Modifier.fillMaxSize().background(BodyBg).verticalScroll(rememberScrollState())) {
        TrackingStatus(resumeCount, Modifier.statusBarsPadding().padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.lg))
        TopSheet {
            SheetContent {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        LabelStack(label = if (signedIn) "Signed in" else "On this device", title = name, meta = email)
                    }
                    Spacer(Modifier.width(Spacing.md))
                    DotAvatar(name.ifBlank { email }, Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            InsetCard {
                Text("All time", style = MaterialTheme.typography.titleLarge.regular, color = Smoke)
                Text(totalNumber?.grouped() ?: total, style = tabular(MaterialTheme.typography.displayMedium), color = Ink, maxLines = 1)
                Text("Best day ${best.grouped()}  ·  $streak-day streak", style = tabular(MaterialTheme.typography.bodyMedium), color = Smoke)
            }
        }

        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("Today")
            GroupedSection {
                GroupedRow("Reels", value = todayReels.grouped()); HairlineDivider()
                GroupedRow("Daily goal", value = target.grouped(), subtitle = "Tap to edit your daily goal", onClick = {
                    goalText = target.toString(); editGoal = true
                }); HairlineDivider()
                GroupedRow("Level", value = ScrollLevel.forCount(todayReels).title)
            }
            goalFeedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BodyMuted) }
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("Last 30 days")
            GroupedSection { DotMatrix(usageHistory) }
            if (!signedIn) {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeading("Join the league")
                SignInButton(onSuccess = { viewModel.refresh(); com.doomly.app.CloudSyncScheduler.scheduleSync(context) })
            }
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("Appearance")
            GroupedSection {
                val appearance = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
                var dark by remember { mutableStateOf(appearance.getBoolean("dark", true)) }
                GroupedRow("Dark mode", showChevron = false, trailing = {
                    BodySwitch(dark, { dark = it; appearance.edit().putBoolean("dark", it).apply() }, "Dark mode")
                })
                HairlineDivider()
                val reduced = LocalReduceMotion.current
                GroupedRow("Reduce motion", subtitle = "Still counters, charts, transitions and celebrations", showChevron = false, trailing = {
                    BodySwitch(reduced, {
                        com.doomly.app.overlay.ReelPillOverlay.preferences(context).edit().putBoolean("reduce_motion", it).apply()
                    }, "Reduce motion")
                })
            }
            Spacer(Modifier.height(Spacing.xxl))
            ReelPillSettings(resumeCount)
            Spacer(Modifier.height(Spacing.xxl))
            NotificationSettings(resumeCount)
            if (BuildConfig.ENABLE_GITHUB_UPDATES) {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeading("Updates")
                GroupedSection {
                    val updatePrefs = remember { context.getSharedPreferences("updates", Context.MODE_PRIVATE) }
                    var beta by remember { mutableStateOf(updatePrefs.getBoolean("beta", false)) }
                    GroupedRow("Beta updates", showChevron = false, trailing = {
                        BodySwitch(
                            beta,
                            { beta = it; updatePrefs.edit().putBoolean("beta", it).apply(); updates?.changeChannel() },
                            "Beta updates",
                            enabled = updateState !is UpdateState.Downloading && updateState != UpdateState.Checking
                        )
                    })
                    HairlineDivider()
                    UpdateRow(updateState, { updates?.checkNow() }, { r -> updates?.download(r) }, { onInstallUpdate() })
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("About")
            GroupedSection {
                GroupedRow("Version", value = BuildConfig.VERSION_NAME)
                HairlineDivider()
                Text(
                    "Counts Reels on-device. Signing in syncs your display name, goal, totals, streak and up to a year of daily Reel counts. Your name and today's count appear in League. No screen text or Reel content is uploaded.",
                    style = MaterialTheme.typography.bodySmall, color = BodyMuted,
                    modifier = Modifier.padding(Spacing.lg)
                )
            }
            if (signedIn) {
                Spacer(Modifier.height(Spacing.lg))
                QuietButton("Sign out", onClick = { confirmSignOut = true }, color = BodyDanger)
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun UpdateRow(state: UpdateState, onCheck: () -> Unit, onDownload: (GitHubRelease) -> Unit, onInstall: (UpdateState.ReadyToInstall) -> Unit) {
    val (label, actionLabel, action) = when (state) {
        UpdateState.Checking -> Triple("Checking…", null, null)
        UpdateState.UpToDate -> Triple("Up to date", "Check", onCheck)
        is UpdateState.Available -> Triple(state.release.versionName + " available", "Download", { onDownload(state.release) })
        is UpdateState.Downloading -> Triple("Downloading · ${state.percent}%", null, null)
        is UpdateState.ReadyToInstall -> Triple("Ready to install", "Install", { onInstall(state) })
        is UpdateState.Failed -> Triple(state.message, "Retry", onCheck)
    }
    GroupedRow(label, trailing = {
        when {
            state == UpdateState.Checking || state is UpdateState.Downloading ->
                CircularProgressIndicator(Modifier.size(18.dp), color = BodyMuted, strokeWidth = 2.dp)
            actionLabel != null && action != null ->
                TextButton(onClick = action) { Text(actionLabel, style = MaterialTheme.typography.titleMedium, color = BodyInk) }
        }
    })
}
