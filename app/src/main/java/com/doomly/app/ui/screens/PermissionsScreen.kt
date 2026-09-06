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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.doomly.app.*
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.components.DottedOrbit
import com.doomly.app.ui.components.StatusDot
import com.doomly.app.ui.theme.*

@Composable
fun PermissionsScreen(resumeCount: Int = 0, onDone: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var signedIn by remember { mutableStateOf(AuthRepository.getInstance().isSignedIn(context)) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(notificationsEnabled(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationGranted = it
    }

    val accessibilityReady = remember(resumeCount) { DeviceReadiness.accessibilityEnabled(context) }
    val completed = listOf(accessibilityReady, signedIn).count { it }

    LaunchedEffect(resumeCount) {
        signedIn = AuthRepository.getInstance().isSignedIn(context)
        notificationGranted = notificationsEnabled(context)
        if (accessibilityReady && signedIn) onDone()
    }

    fun continueSetup() {
        when {
            !accessibilityReady -> showAccessibilityDisclosure = true
            !signedIn -> (activity as? MainActivity)?.let { host ->
                AuthRepository.getInstance().signIn(host, object : ResultCallback<Void> {
                    override fun onSuccess(value: Void?) {
                        signedIn = true
                        authError = null
                        if (accessibilityReady) onDone()
                    }
                    override fun onError(message: String) { authError = message }
                })
            }
            else -> onDone()
        }
    }

    Column(
        Modifier.fillMaxSize().background(Void).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(26.dp))
        Text("Doomly", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(3.dp))
        Text("Reel tracking, with suspiciously good posture.", style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
        DottedOrbit(completed / 2f, Modifier.fillMaxWidth().height(170.dp), "Setup progress")

        Text("Make Doomly yours", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text("These are optional. You can start now and turn them on whenever you’re ready.", style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("OPTIONAL SETUP", style = MaterialTheme.typography.labelSmall, color = Muted)
            Text("$completed OF 2", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(horizontal = 15.dp)) {
            SetupRow(Icons.Rounded.Visibility, "Recognize Reels", "Reads visible Instagram UI locally to identify Reels.", accessibilityReady)
            HorizontalDivider(color = Hairline)
            SetupRow(Icons.Rounded.PersonOutline, "Save your score", "Sign in for cloud sync and the daily league.", signedIn)
        }

        authError?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Danger, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = ::continueSetup,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Frost, contentColor = Void)
        ) {
            Text(nextLabel(accessibilityReady, signedIn))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
        }

        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Skip for now", color = Muted)
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            enabled = !notificationGranted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.NotificationsNone, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (notificationGranted) "Gentle reminders enabled" else "Enable reminders (optional)")
        }
        Spacer(Modifier.height(8.dp))
        Text("You can change every permission later in Android Settings.", style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
    }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = { Text("Before you enable Reel recognition") },
            text = {
                Text(
                    "Doomly uses Android Accessibility to inspect visible Instagram screen labels and controls so it can recognize when a Reel is shown. " +
                        "It creates a short local fingerprint from visible creator and caption text to avoid duplicate counts. " +
                        "Original screen text is not stored or uploaded; only aggregate Reel counts sync to your account. " +
                        "Doomly does not tap, type, message, or control Instagram."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityDisclosure = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("I understand — continue") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) { Text("Not now") }
            }
        )
    }
}

@Composable
private fun SetupRow(icon: ImageVector, title: String, body: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(if (done) Mint.copy(alpha = .35f) else PanelRaised, CircleShape), Alignment.Center) {
            Icon(icon, null, tint = Frost, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        StatusDot(done)
    }
}

private fun nextLabel(accessibility: Boolean, signedIn: Boolean) = when {
    !accessibility -> "Allow Reel recognition"
    !signedIn -> "Sign in and finish"
    else -> "Open dashboard"
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
