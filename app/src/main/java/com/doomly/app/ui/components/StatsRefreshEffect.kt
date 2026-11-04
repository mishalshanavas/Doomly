package com.doomly.app.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.doomly.app.DoomStatsStore
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

/** Refresh on local writes, foreground entry, and midnight without polling the cloud. */
@Composable
fun StatsRefreshEffect(refresh: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { latest() }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(DoomStatsStore.ACTION_STATS_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            latest()
            while (true) {
                val now = ZonedDateTime.now()
                val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay(Duration.between(now, next).toMillis().coerceAtLeast(1))
                latest()
            }
        }
    }
}
