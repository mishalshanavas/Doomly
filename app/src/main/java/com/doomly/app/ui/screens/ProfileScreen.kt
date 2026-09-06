package com.doomly.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomly.app.BuildConfig
import com.doomly.app.DoomStatsStore
import com.doomly.app.ProfileViewModel
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.PageHeader
import com.doomly.app.ui.components.PremiumCard
import com.doomly.app.ui.components.DailyUsageHeatmap
import com.doomly.app.ui.theme.*
import com.doomly.app.update.GitHubUpdateManager
import com.doomly.app.update.UpdateState

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(), onSignOut: () -> Unit = {}) {
    val owner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val name by viewModel.getDisplayName().observeAsState("Doomly user")
    val initials by viewModel.getInitials().observeAsState("D")
    val email by viewModel.getEmail().observeAsState("Local profile")
    val streak by viewModel.getStreak().observeAsState("0")
    val target by viewModel.getDailyTarget().observeAsState(DoomStatsStore.DEFAULT_DAILY_TARGET)
    val total by viewModel.getTotalReels().observeAsState("0")
    val signedIn by viewModel.getSignedIn().observeAsState(false)
    var confirmSignOut by remember { mutableStateOf(false) }
    val updateManager = remember(context) { GitHubUpdateManager(context) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }

    LaunchedEffect(updateManager) {
        if (BuildConfig.ENABLE_GITHUB_UPDATES) updateManager.check { updateState = it }
    }

    LaunchedEffect(updateState) {
        val ready = updateState as? UpdateState.ReadyToInstall ?: return@LaunchedEffect
        context.findActivity()?.let { updateManager.install(it, ready) }
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        owner.lifecycle.addObserver(observer)
        viewModel.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(updateManager) { onDispose(updateManager::cancel) }
    val best = DoomStatsStore.dailyHistory(context).values.maxOrNull() ?: 0
    val usageHistory = DoomStatsStore.dailyHistory(context).toMutableMap().apply {
        put(java.time.LocalDate.now().toString(), DoomStatsStore.snapshot(context).todayReels)
    }

    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text("Sign out of Doomly?") },
        text = { Text("Your local counts stay on this device, but cloud sync and the league will pause.") },
        confirmButton = {
            TextButton(onClick = {
                AuthRepository.getInstance().signOut(context)
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
        PageHeader("Profile", "The person behind the highly trained thumb.")
        Spacer(Modifier.height(26.dp))

        Box(Modifier.size(82.dp).background(Frost, CircleShape), Alignment.Center) {
            Text(initials, style = MaterialTheme.typography.headlineMedium, color = Void)
        }
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(email, style = MaterialTheme.typography.bodySmall, color = Muted)
        Spacer(Modifier.height(22.dp))

        PremiumCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("LIFETIME REELS", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(5.dp))
                    Text(total, style = MaterialTheme.typography.displayMedium)
                }
                Icon(Icons.Rounded.AutoAwesome, null, tint = Frost, modifier = Modifier.padding(bottom = 7.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("A number best enjoyed without context.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileStat(streak, "DAY STREAK", Modifier.weight(1f))
            ProfileStat(target.toString(), "DAILY GOAL", Modifier.weight(1f))
            ProfileStat(best.toString(), "BEST DAY", Modifier.weight(1f))
        }

        Spacer(Modifier.height(18.dp))
        PremiumCard(Modifier.fillMaxWidth()) {
            DailyUsageHeatmap(usageHistory, Modifier.fillMaxWidth())
        }

        if (BuildConfig.ENABLE_GITHUB_UPDATES) {
            Spacer(Modifier.height(22.dp))
            Text("APP UPDATE", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            UpdateCard(
                state = updateState,
                onCheck = { updateManager.check { updateState = it } },
                onDownload = { release -> updateManager.download(release) { updateState = it } },
                onInstall = { ready ->
                    val started = context.findActivity()?.let { updateManager.install(it, ready) } == true
                    if (!started) updateState = UpdateState.Failed("Android could not open the package installer.")
                }
            )
        }

        Spacer(Modifier.height(22.dp))
        Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, color = Muted, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).background(Frost, CircleShape), Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = Void, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Cloud sync", style = MaterialTheme.typography.titleSmall)
                Text(if (signedIn) "Signed in and keeping score" else "Local-only mode", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Text(if (signedIn) "ON" else "OFF", style = MaterialTheme.typography.labelSmall)
        }

        if (signedIn) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmSignOut = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
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
            body = "A signed update is ready on GitHub. Android will ask before installing it."
            button = "Download update"
            enabled = true
            action = { onDownload(state.release) }
        }
        is UpdateState.Downloading -> {
            title = "Downloading ${state.release.versionName}"
            body = "${state.percent}% complete. Tiny package, large ambitions."
            button = "Downloading…"
            enabled = false
            action = {}
        }
        is UpdateState.ReadyToInstall -> {
            title = "Update verified"
            body = "Package name, version, and signing certificate all match."
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

    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(15.dp)) {
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
            shape = RoundedCornerShape(12.dp),
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
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(13.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, maxLines = 1)
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
