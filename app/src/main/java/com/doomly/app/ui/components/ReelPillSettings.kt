package com.doomly.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doomly.app.DeviceReadiness
import com.doomly.app.overlay.ReelPillOverlay
import com.doomly.app.ui.theme.*

@Composable
fun ReelPillSettings(refreshKey: Int = 0) {
    val context = LocalContext.current
    val prefs = remember(context) { ReelPillOverlay.preferences(context) }
    var requested by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    val permission = remember(refreshKey, requested) { Settings.canDrawOverlays(context) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Reel celebrations", style = MaterialTheme.typography.bodyMedium, color = Frost)
                Spacer(Modifier.height(4.dp))
                Text("A small pill below the camera at every 10 Reels.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = requested && permission, onCheckedChange = { checked ->
                requested = checked
                prefs.edit().putBoolean("enabled", checked).apply()
                error = null
                if (checked && !Settings.canDrawOverlays(context)) {
                    try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
                    catch (_: RuntimeException) { requested = false; prefs.edit().putBoolean("enabled", false).apply(); error = "Couldn't open overlay settings on this device." }
                }
            })
        }
        if (requested && !permission) Text("Allow display over other apps in Android Settings to enable the pill.", style = MaterialTheme.typography.bodySmall, color = Muted)
        if (requested && permission && !DeviceReadiness.accessibilityEnabled(context)) Text("Enable Reel tracking on Today to show celebrations.", style = MaterialTheme.typography.bodySmall, color = Muted)
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Danger) }
    }
}
