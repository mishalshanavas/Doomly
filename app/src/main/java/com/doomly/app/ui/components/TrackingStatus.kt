package com.doomly.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doomly.app.DeviceReadiness
import com.doomly.app.ui.theme.*

@Composable
fun TrackingStatus(refreshKey: Int = 0) {
    val context = LocalContext.current
    var disclosure by remember { mutableStateOf(false) }
    val tracking = remember(refreshKey) { DeviceReadiness.accessibilityEnabled(context) }
    if (!tracking) {
        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Tracking paused", style = MaterialTheme.typography.titleMedium)
            Text("Your saved counts are safe. Enable tracking to count new Reels.", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { disclosure = true }) { Text("Enable tracking") }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (disclosure) TrackingDisclosure(onDismiss = { disclosure = false })
}

@Composable
fun TrackingDisclosure(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Before enabling Reel recognition") },
        text = { Text("Doomly uses Android Accessibility to inspect visible Instagram screen labels and controls to recognize Reels. It creates a short local fingerprint from visible creator and caption text to avoid duplicate counts. Original screen text is not stored or uploaded; only aggregate Reel counts sync when you sign in. Doomly does not tap, type, message, or control Instagram. It also checks whether Instagram remains in the foreground to identify long sessions and offer optional Reel reminders. Content from other apps is never read. If you enable Reel celebrations, a small overlay appears at each 10-Reel milestone while Reels are visible. You can disable tracking in Android Settings anytime.") },
        confirmButton = { TextButton(onClick = { onDismiss(); context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("I understand—open Settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } })
}
