package com.doomly.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doomly.app.DeviceReadiness
import com.doomly.app.ui.theme.*

/** One quiet status line when tracking is on; a single actionable row when paused. */
@Composable
fun TrackingStatus(refreshKey: Int = 0) {
    val context = LocalContext.current
    var disclosure by remember { mutableStateOf(false) }
    val tracking = remember(refreshKey) { DeviceReadiness.accessibilityEnabled(context) }
    if (tracking) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(BodyAccent))
            Spacer(Modifier.width(Spacing.sm))
            Text("Tracking on", style = MaterialTheme.typography.bodyMedium, color = BodyMuted)
        }
    } else {
        GroupedSection {
            GroupedRow(
                "Tracking paused",
                subtitle = "Counts are saved on this device.",
                onClick = { disclosure = true },
                trailing = { Text("Turn on", style = MaterialTheme.typography.titleMedium, color = BodyAccent) }
            )
        }
    }
    if (disclosure) TrackingDisclosure(onDismiss = { disclosure = false })
}

@Composable
fun TrackingDisclosure(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var settingsError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DoomlyShapes.extraLarge,
        containerColor = Sheet,
        title = { Text("Reel tracking", style = MaterialTheme.typography.headlineSmall, color = Ink) },
        text = {
            Text(
                if (settingsError) "Couldn't open Settings. Open Android Settings → Accessibility → Doomly to turn on tracking, or choose Not now."
                else "Doomly uses Accessibility to recognise Reels in Instagram. Screen text is not stored or uploaded; only totals sync if you sign in.",
                style = MaterialTheme.typography.bodyMedium, color = Smoke
            )
        },
        confirmButton = {
            TextButton(onClick = {
                try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); onDismiss() }
                catch (_: RuntimeException) { settingsError = true }
            }) { Text("Open Settings", color = Ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now", color = Smoke) } }
    )
}
