package com.doomly.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.doomly.app.DeviceReadiness
import com.doomly.app.ui.theme.*

/**
 * Silent while tracking is on — no label, no dot. When tracking is off, one
 * warning row (a "!" badge) goes at the top of the screen instead.
 */
@Composable
fun TrackingStatus(refreshKey: Int = 0, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var disclosure by remember { mutableStateOf(false) }
    val tracking = remember(refreshKey) { DeviceReadiness.accessibilityEnabled(context) }
    if (!tracking) {
        GroupedSection(modifier) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable { disclosure = true }
                    .heightIn(min = 52.dp)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    .semantics { contentDescription = "Warning: tracking is off. Reels will not be counted." },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(BodyDanger),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", style = MaterialTheme.typography.titleMedium, color = BodyInk)
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text("Tracking is off", style = MaterialTheme.typography.bodyLarge, color = BodyInk)
                    Text("Reels will not be counted.", style = MaterialTheme.typography.bodySmall, color = BodyMuted)
                }
                Text("Turn on", style = MaterialTheme.typography.titleMedium, color = BodyInk)
            }
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
