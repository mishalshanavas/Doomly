package com.doomly.app.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.doomly.app.overlay.ReelPillOverlay

val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { ReelPillOverlay.preferences(context) }
    var reduced by remember { mutableStateOf(prefs.getBoolean("reduce_motion", false)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "reduce_motion") reduced = prefs.getBoolean("reduce_motion", false)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return reduced
}

@Composable
fun motionDuration(milliseconds: Int): Int = if (LocalReduceMotion.current) 0 else milliseconds
