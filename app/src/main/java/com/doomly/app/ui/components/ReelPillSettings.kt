package com.doomly.app.ui.components

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.doomly.app.R
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    val preview = remember(context) { ReelPillOverlay(context) { true } }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(preview, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) preview.hide()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); preview.close() }
    }
    var trackingDisclosure by remember { mutableStateOf(false) }
    fun requestOverlayPermission() {
        error = null
        try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
        catch (_: RuntimeException) { error = "Couldn't open overlay settings. Look for Doomly under Display over other apps in Android Settings." }
    }
    var camera by remember { mutableStateOf(prefs.getBoolean("camera_aligned", true)) }
    var haptics by remember { mutableStateOf(prefs.getBoolean("haptics", false)) }
    var reduced by remember { mutableStateOf(prefs.getBoolean("reduce_motion", false)) }
    var offset by remember { mutableFloatStateOf(prefs.getInt("offset", 0).toFloat()) }
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Hairline)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_coach), "Mochi", Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mochi, your scroll coach", style = MaterialTheme.typography.bodyMedium, color = Frost)
                Spacer(Modifier.height(4.dp))
                Text("A tiny companion cheering every 10 Reels this session.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.width(12.dp))
            Switch(modifier = Modifier.semantics { contentDescription = "Mochi celebrations" }, checked = requested, onCheckedChange = { checked ->
                requested = checked
                prefs.edit().putBoolean("enabled", checked).apply()
                error = null
                if (!checked) preview.hide()
                if (checked && !Settings.canDrawOverlays(context)) requestOverlayPermission()
            })
        }
        if (requested && !permission) {
            Text("Setup pending · allow display over other apps to activate Mochi.", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { requestOverlayPermission() }) { Text("Allow overlay") }
        }
        if (requested && permission && !DeviceReadiness.accessibilityEnabled(context)) {
            Text("Tracking is paused. You can preview Mochi, but celebrations need Reel tracking.", style = MaterialTheme.typography.bodySmall, color = Muted)
            TextButton(onClick = { trackingDisclosure = true }) { Text("Enable tracking") }
        }
        if (requested && permission) {
            Text("Tap or swipe to dismiss. Hold to snooze for this session.", style = MaterialTheme.typography.bodySmall, color = Muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Align with camera", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(modifier = Modifier.semantics { contentDescription = "Align with camera" }, checked = camera, onCheckedChange = { camera = it; prefs.edit().putBoolean("camera_aligned", it).apply() })
            }
            Text(if (offset < 1) "Position · just below camera" else "Position · lower on screen", style = MaterialTheme.typography.bodySmall, color = Muted)
            Slider(modifier = Modifier.semantics { contentDescription = "Overlay distance below camera" }, value = offset, onValueChange = { offset = it }, valueRange = 0f..120f,
                onValueChangeFinished = { prefs.edit().putInt("offset", offset.toInt()).apply() })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gentle haptics", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(modifier = Modifier.semantics { contentDescription = "Gentle haptics" }, checked = haptics, onCheckedChange = { haptics = it; prefs.edit().putBoolean("haptics", it).apply() })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce motion", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(modifier = Modifier.semantics { contentDescription = "Reduce motion" }, checked = reduced, onCheckedChange = { reduced = it; prefs.edit().putBoolean("reduce_motion", it).apply() })
            }
            TextButton(onClick = { preview.preview() }) { Text("Preview Mochi") }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Danger) }
    }
    if (trackingDisclosure) TrackingDisclosure { trackingDisclosure = false }
}
