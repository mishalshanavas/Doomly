package com.doomly.app.ui.components

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import kotlinx.coroutines.delay

/** A deliberately unproductive, optional minute. No notifications or background work. */
@Composable
fun NothingBreak() {
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var remaining by rememberSaveable { mutableIntStateOf(60) }
    LaunchedEffect(deadline) {
        if (deadline == 0L) return@LaunchedEffect
        while (true) {
            remaining = ((deadline - SystemClock.elapsedRealtime() + 999) / 1000).toInt().coerceIn(0, 60)
            if (remaining == 0) break
            delay(200)
        }
    }
    Spacer(Modifier.height(24.dp))
    PremiumCard(Modifier.fillMaxWidth()) {
        Text("THE NOTHING CLUB", style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(10.dp))
        Text(if (deadline == 0L) "Busy doing nothing." else if (remaining == 0) "Beautifully unproductive." else "${remaining}s of absolutely nothing.",
            style = MaterialTheme.typography.headlineSmall, color = Frost)
        Spacer(Modifier.height(8.dp))
        Text("No score. No streak. Just a minute that's yours.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            if (deadline != 0L && remaining > 0) { deadline = 0L; remaining = 60 }
            else { remaining = 60; deadline = SystemClock.elapsedRealtime() + 60_000 }
        }) { Text(if (deadline != 0L && remaining > 0) "End break" else "Take a lazy minute") }
    }
}
