package com.doomly.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.doomly.app.*
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.MoodFace
import com.doomly.app.ui.components.SoftCard
import com.doomly.app.ui.components.StatusDot
import com.doomly.app.ui.theme.*

@Composable
fun PermissionsScreen(resumeCount: Int = 0, onDone: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var signedIn by remember { mutableStateOf(AuthRepository.getInstance().isSignedIn(context)) }
    var authError by remember { mutableStateOf<String?>(null) }
    var notificationGranted by remember { mutableStateOf(notificationsEnabled(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }

    val accessibilityReady = remember(resumeCount) { DeviceReadiness.accessibilityEnabled(context) }
    val batteryReady = remember(resumeCount) { DeviceReadiness.batteryOptimizationDisabled(context) }
    LaunchedEffect(resumeCount) {
        signedIn = AuthRepository.getInstance().isSignedIn(context)
        notificationGranted = notificationsEnabled(context)
        if (accessibilityReady && batteryReady && signedIn) onDone()
    }

    Column(Modifier.fillMaxSize().background(Sunshine).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 34.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Meet Doomly", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text("A tiny companion for your Reel journey", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = .64f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            MoodFace(.5f, Modifier.width(150.dp))
            Text("Let's get you ready", style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)).background(Paper)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text("Three quick steps", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text("Doomly needs these to count Reels reliably in the background.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(18.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                SetupRow("1", "Reel tracking", "Allow Doomly to recognize Instagram Reels.", accessibilityReady, if (accessibilityReady) "Ready" else "Enable") {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)
                SetupRow("2", "Background battery", "Keep counting when Doomly is not open.", batteryReady, if (batteryReady) "Ready" else "Allow") {
                    activity?.let(DeviceReadiness::requestBatteryExemption)
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)
                SetupRow("3", "Your profile", "Save progress and join the leaderboard.", signedIn, if (signedIn) "Ready" else "Sign in") {
                    (activity as? MainActivity)?.let { host ->
                        AuthRepository.getInstance().signIn(host, object : ResultCallback<Void> {
                            override fun onSuccess(value: Void?) { signedIn = true; authError = null; onDone() }
                            override fun onError(message: String) { authError = message }
                        })
                    }
                }
            }
            authError?.let { Text(it, color = Coral, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                enabled = !notificationGranted,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp)
            ) { Text(if (notificationGranted) "Motivation reminders enabled" else "Enable motivation reminders") }
            Spacer(Modifier.height(12.dp))
            Text("Battery permission is checked again every time you return from Settings.", style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SetupRow(number: String, title: String, body: String, done: Boolean, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(SunshineSoft, RoundedCornerShape(11.dp)), Alignment.Center) { Text(number, style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        if (done) StatusDot(true) else TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(action) }
    }
}

private fun notificationsEnabled(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
