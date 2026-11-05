package com.doomly.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun PermissionsScreen(resumeCount: Int = 0, onDone: () -> Unit = {}) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf("") }
    var disclosure by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    fun finish() { SetupPreferences.finish(context); onDone() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) finish() else denied = true
    }
    LaunchedEffect(resumeCount, step) {
        if (step == 1 && DeviceReadiness.accessibilityEnabled(context)) step = 2
        if (step == 2 && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) finish()
    }
    Column(Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        TurnHeader("Hello, player.")
        Spacer(Modifier.height(24.dp))
        BoardArt(com.doomly.app.R.drawable.board_pawn, Modifier.size(90.dp))
        Spacer(Modifier.height(20.dp))
        Text("Step ${step + 1} of 3", style = MaterialTheme.typography.labelMedium, color = Muted)
        Spacer(Modifier.height(12.dp))
        PremiumCard(Modifier.fillMaxWidth()) {
            Text(when(step) { 0 -> "Choose your first daily goal"; 1 -> "Let Doomly count your Reels"; else -> "Want notifications?" }, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(when(step) { 0 -> "Your thumb, your ambition. You can change this anytime."; 1 -> "Tracking is optional. Your history stays available even when it's paused."; else -> "Notifications are optional. You can change them later in Profile." }, style = MaterialTheme.typography.bodyMedium, color = Muted)
            if (step == 0) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(goal, { goal = it.filter(Char::isDigit).take(5) }, label = { Text("Reels per day") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = goal.isNotEmpty() && goal.toIntOrNull() !in 1..20_000,
                    supportingText = { Text("Choose between 1 and 20,000.") }, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            when(step) {
                0 -> { DoomStatsStore.setDailyTarget(context, goal.toInt()); step = 1 }
                1 -> disclosure = true
                else -> if (denied || Build.VERSION.SDK_INT < 33) context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }, enabled = step != 0 || goal.toIntOrNull() in 1..20_000, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(when(step) { 0 -> "Set my goal"; 1 -> "Enable tracking"; else -> if (denied) "Open notification settings" else "Enable notifications" })
        }
        if (step > 0) TextButton(onClick = { if (step == 1) step = 2 else finish() }) { Text("Not now") }
        if (denied && step == 2) Text("No problem—you can keep using Doomly without notifications.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Text("No account needed. Sign in later to join the league.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
    if (disclosure) TrackingDisclosure(onDismiss = { disclosure = false })
}
