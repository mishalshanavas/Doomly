package com.doomly.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
    val updateState = updates?.state ?: UpdateState.UpToDate
    StatsRefreshEffect { viewModel.refresh() }
    val todayReels = DoomStatsStore.snapshot(context).todayReels
    val history = DoomStatsStore.dailyHistory(context)
    val best = maxOf(history.values.maxOrNull() ?: 0, todayReels)
    val usageHistory = history + (java.time.LocalDate.now().toString() to todayReels)
    val totalNumber = total.toIntOrNull()

    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false }, shape = DoomlyShapes.extraLarge, containerColor = Sheet,
        title = { Text("Sign out?", style = MaterialTheme.typography.headlineSmall, color = Ink) },
        text = { Text("Counts stay on this device.", style = MaterialTheme.typography.bodyMedium, color = Smoke) },
        confirmButton = { TextButton(onClick = { AuthRepository.getInstance().signOut(context); viewModel.refresh(); confirmSignOut = false; onSignOut() }) { Text("Sign out", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel", color = Smoke) } })

    Column(Modifier.fillMaxSize().background(BodyBg).verticalScroll(rememberScrollState())) {
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
                GroupedRow("Daily goal", value = target.grouped()); HairlineDivider()
                GroupedRow("Level", value = ScrollLevel.forCount(todayReels).title)
            }
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("Last 30 days")
            GroupedSection { DotMatrix(usageHistory) }
            Spacer(Modifier.height(Spacing.xxl))
            TrackingStatus(resumeCount)
            if (!signedIn) {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeading("Join the league")
                SignInButton(onSuccess = { viewModel.refresh(); com.doomly.app.CloudSyncScheduler.scheduleSync(context) })
            }
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeading("Appearance")
            GroupedSection {
                val appearance = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
                var dark by remember { mutableStateOf(appearance.getBoolean("dark", false)) }
                GroupedRow("Dark mode", showChevron = false, trailing = {
                    BodySwitch(dark, { dark = it; appearance.edit().putBoolean("dark", it).apply() }, "Dark mode")
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
                    "Counts Reels on-device. Signing in syncs name and totals only.",
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
