package com.doomly.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doomly.app.DoomlyNotifications
import com.doomly.app.ui.theme.*

@Composable
fun NotificationSettings(refreshKey: Int = 0) {
    val context = LocalContext.current
    val allowed = remember(refreshKey) { androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val categories = remember(refreshKey) {
        listOf(DoomlyNotifications.MILESTONES to "Milestones", DoomlyNotifications.UPDATES to "App updates")
            .map { (id, title) -> Triple(id, title, DoomlyNotifications.enabled(context, id)) }
    }
    Column(Modifier.fillMaxWidth()) {
        Text("NOTIFICATIONS", style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(8.dp))
        if (!allowed) Text("Notifications are off. Doomly still works normally.", style = MaterialTheme.typography.bodySmall, color = Muted)
        categories.forEach { (channel, title, enabled) ->
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, channel)) },
                modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                Text(title, modifier = Modifier.weight(1f), color = Frost)
                Text(if (enabled) "On  ›" else "Off  ›", color = Muted)
            }
        }
        if (!allowed) TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Enable in Android Settings") }
    }
}
