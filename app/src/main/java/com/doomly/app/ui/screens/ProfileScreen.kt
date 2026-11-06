package com.doomly.app.ui.screens

import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
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
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.SignInButton
import com.doomly.app.ui.components.ReelPillSettings
import com.doomly.app.ui.components.NotificationSettings
import com.doomly.app.CloudSyncScheduler
import com.doomly.app.ui.components.GeneratedAvatar
import com.doomly.app.ui.components.EditorialSection
import com.doomly.app.ui.components.PageHeader
import com.doomly.app.ui.components.PremiumCard
import com.doomly.app.ui.components.DailyUsageHeatmap
import com.doomly.app.ui.theme.*
import com.doomly.app.update.AppUpdateViewModel
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
    val localAvatarSeed = remember(context) {
        val prefs = context.getSharedPreferences("doomly_identity", Context.MODE_PRIVATE)
        prefs.getString("avatar_seed", null) ?: java.util.UUID.randomUUID().toString().also {
            prefs.edit().putString("avatar_seed", it).apply()
        }
    }
    val avatarSeed = if (signedIn) AuthRepository.getInstance().currentUid(context).ifBlank { email } else localAvatarSeed
    var confirmSignOut by remember { mutableStateOf(false) }
    val updateState = updates?.state ?: UpdateState.UpToDate

    com.doomly.app.ui.components.StatsRefreshEffect { viewModel.refresh() }
    val best = maxOf(DoomStatsStore.dailyHistory(context).values.maxOrNull() ?: 0, DoomStatsStore.snapshot(context).todayReels)
    val usageHistory = DoomStatsStore.dailyHistory(context).toMutableMap().apply {
        put(java.time.LocalDate.now().toString(), DoomStatsStore.snapshot(context).todayReels)
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out of Doomly?") },
        text = { Text("Your counts stay on this device. Sign in again whenever you want to rejoin the league.") },
        confirmButton = {
            TextButton(onClick = {
                AuthRepository.getInstance().signOut(context)
                viewModel.refresh()
                confirmSignOut = false
                onSignOut()
            }) { Text("Sign out", color = Danger) }
        },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") } }
    )

    Column(
        Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        PageHeader("Profile")
        Spacer(Modifier.height(26.dp))

        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            GeneratedAvatar(avatarSeed, Modifier.size(52.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(email, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Hairline)

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat(total, "Lifetime Reels", Modifier.weight(1f))
            ProfileStat(streak, "Day streak", Modifier.weight(1f))
        }
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat(target.toString(), "Daily goal", Modifier.weight(1f))
            ProfileStat(best.toString(), "Most Reels in a day", Modifier.weight(1f))
        }
        HorizontalDivider(color = Hairline)
        Spacer(Modifier.height(24.dp))
        EditorialSection("", "History")
        com.doomly.app.ui.components.ScrollCompanion(DoomStatsStore.snapshot(context).todayReels)
        com.doomly.app.ui.components.DailyAchievementBadge(DoomStatsStore.snapshot(context).todayReels, target)
        DailyUsageHeatmap(usageHistory, Modifier.fillMaxWidth())
        if (BuildConfig.ENABLE_GITHUB_UPDATES) {
            Spacer(Modifier.height(22.dp))
            val updatePrefs = remember { context.getSharedPreferences("updates", Context.MODE_PRIVATE) }
            var beta by remember { mutableStateOf(updatePrefs.getBoolean("beta", false)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Beta updates", style = MaterialTheme.typography.titleMedium)
                    Text("Early builds from GitHub. Turning off waits for a newer stable release. Android confirms installation.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Switch(checked = beta, onCheckedChange = {
                    beta = it
                    updatePrefs.edit().putBoolean("beta", it).apply()
                    updates?.changeChannel()
                }, enabled = updateState !is UpdateState.Downloading && updateState != UpdateState.Checking)
            }
            UpdateCard(
                state = updateState,
                onCheck = { updates?.checkNow() },
                onDownload = { release -> updates?.download(release) },
                onInstall = { onInstallUpdate() }
            )
        }

        Spacer(Modifier.height(22.dp))
        if (!signedIn) {
            Text("Join the league", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text("Sign in to join the league.", style = MaterialTheme.typography.bodySmall, color = Muted,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            SignInButton(onSuccess = {
                viewModel.refresh()
                CloudSyncScheduler.scheduleSync(context)
            })
        }
        Spacer(Modifier.height(16.dp))
        com.doomly.app.ui.components.TrackingStatus(resumeCount)
        Spacer(Modifier.height(16.dp))
        EditorialSection("", "Settings")
        val appearance = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
        var dark by remember { mutableStateOf(appearance.getBoolean("dark", false)) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Dark appearance", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = dark, onCheckedChange = { dark = it; appearance.edit().putBoolean("dark", it).apply() })
        }
        ReelPillSettings(resumeCount)
        Spacer(Modifier.height(16.dp))
        NotificationSettings(resumeCount)
        Spacer(Modifier.height(24.dp))
        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Your data, plainly", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Doomly counts Instagram Reels on this device. Signing in syncs your name and aggregate counts for the league. Screen text and video content are not uploaded. Tracking can be disabled in Android Accessibility settings.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(16.dp))
        Text("Doomly ${BuildConfig.VERSION_NAME} · ${if (BuildConfig.DEBUG) "Beta / debug" else "Stable"}", style = MaterialTheme.typography.bodySmall, color = Muted)
        if (signedIn) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign out")
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun UpdateCard(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (com.doomly.app.update.GitHubRelease) -> Unit,
    onInstall: (UpdateState.ReadyToInstall) -> Unit
) {
    if (state == UpdateState.UpToDate || state == UpdateState.Checking) {
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Doomly ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text(if (state == UpdateState.Checking) "Checking for updates…" else "Up to date",
                    style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            TextButton(onClick = onCheck, enabled = state != UpdateState.Checking) { Text("Check") }
        }
        return
    }
    val title: String
    val body: String
    val button: String
    val enabled: Boolean
    val action: () -> Unit
    when (state) {
        UpdateState.Checking -> {
            title = "Checking for updates"
            body = "Asking GitHub if Doomly has learned any new tricks."
            button = "Checking…"
            enabled = false
            action = {}
        }
        UpdateState.UpToDate -> {
            title = "Doomly ${BuildConfig.VERSION_NAME}"
            body = "You have the latest GitHub release. Very current of you."
            button = "Check again"
            enabled = true
            action = onCheck
        }
        is UpdateState.Available -> {
            title = "Doomly ${state.release.versionName} is available"
            body = "Downloading automatically. Android will ask before installing it."
            button = "Download update"
            enabled = true
            action = { onDownload(state.release) }
        }
        is UpdateState.Downloading -> {
            title = "Downloading ${state.release.versionName}"
            body = "${state.percent}% complete. You can keep using Doomly."
            button = "Downloading…"
            enabled = false
            action = {}
        }
        is UpdateState.ReadyToInstall -> {
            title = "Update verified"
            body = "Verified and ready. Confirm installation in Android."
            button = "Install update"
            enabled = true
            action = { onInstall(state) }
        }
        is UpdateState.Failed -> {
            title = "Could not update"
            body = state.message
            button = "Try again"
            enabled = true
            action = onCheck
        }
    }

    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(PanelRaised, CircleShape), Alignment.Center) {
                Icon(
                    if (state is UpdateState.UpToDate) Icons.Rounded.Check else Icons.Rounded.SystemUpdateAlt,
                    null,
                    tint = Frost,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = action,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Frost, contentColor = Void)
        ) {
            Icon(
                if (state is UpdateState.UpToDate || state is UpdateState.Failed) Icons.Rounded.Refresh else Icons.Rounded.Download,
                null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(button)
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().padding(vertical = 18.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
