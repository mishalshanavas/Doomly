package com.doomly.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doomly.app.DeviceReadiness
import com.doomly.app.overlay.ReelPillOverlay
import com.doomly.app.ui.theme.*

/** Celebration overlay settings; details appear only once enabled and permitted. */
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
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) preview.hide() }
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

    SectionHeading("Celebrations")
    GroupedSection {
        GroupedRow("Show over Instagram", subtitle = "A short pill every 10 reels", showChevron = false, trailing = {
            BodySwitch(requested, { checked ->
                requested = checked
                prefs.edit().putBoolean("enabled", checked).apply()
                error = null
                if (!checked) preview.hide()
                if (checked && !Settings.canDrawOverlays(context)) requestOverlayPermission()
            }, "Show over Instagram")
        })
        if (requested && !permission) {
            HairlineDivider()
            GroupedRow("Allow display over other apps", onClick = { requestOverlayPermission() })
        }
        if (requested && permission && !DeviceReadiness.accessibilityEnabled(context)) {
            HairlineDivider()
            GroupedRow("Turn on Reel tracking", subtitle = "Celebrations need tracking", onClick = { trackingDisclosure = true })
        }
        if (requested && permission) {
            HairlineDivider()
            GroupedRow("Align with camera", showChevron = false, trailing = {
                BodySwitch(camera, { camera = it; prefs.edit().putBoolean("camera_aligned", it).apply() }, "Align with camera")
            })
            HairlineDivider()
            Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text("Position", style = MaterialTheme.typography.bodyLarge, color = BodyInk)
                Text(if (offset < 1) "Just below the camera" else "Lower on screen", style = MaterialTheme.typography.bodySmall, color = BodyMuted)
                Slider(
                    modifier = Modifier.semantics { contentDescription = "Overlay distance below camera" },
                    value = offset, onValueChange = { offset = it }, valueRange = 0f..120f,
                    onValueChangeFinished = { prefs.edit().putInt("offset", offset.toInt()).apply() },
                    colors = SliderDefaults.colors(thumbColor = BodyInk, activeTrackColor = BodyInk, inactiveTrackColor = BodyFill)
                )
            }
            HairlineDivider()
            GroupedRow("Haptics", showChevron = false, trailing = {
                BodySwitch(haptics, { haptics = it; prefs.edit().putBoolean("haptics", it).apply() }, "Haptics")
            })
            HairlineDivider()
            GroupedRow("Reduce motion", showChevron = false, trailing = {
                BodySwitch(reduced, { reduced = it; prefs.edit().putBoolean("reduce_motion", it).apply() }, "Reduce motion")
            })
            HairlineDivider()
            GroupedRow("Preview", onClick = { preview.preview() })
        }
        error?.let {
            HairlineDivider()
            Text(it, style = MaterialTheme.typography.bodySmall, color = BodyDanger, modifier = Modifier.padding(Spacing.lg))
        }
    }
    if (trackingDisclosure) TrackingDisclosure { trackingDisclosure = false }
}
