package com.doomly.app.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.doomly.app.DoomlyNotifications

/** Notification channels as dark rows; each opens its Android channel settings. */
@Composable
fun NotificationSettings(refreshKey: Int = 0) {
    val context = LocalContext.current
    val allowed = remember(refreshKey) { androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val categories = remember(refreshKey) {
        listOf(DoomlyNotifications.MILESTONES to "Milestones", DoomlyNotifications.UPDATES to "App updates", DoomlyNotifications.REEL_REMINDERS to "Reel reminders")
            .map { (id, title) -> Triple(id, title, DoomlyNotifications.enabled(context, id)) }
    }
    SectionHeading("Notifications")
    GroupedSection {
        if (!allowed) {
            GroupedRow(
                "Notifications are off",
                subtitle = "Turn them on in Android settings",
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            )
        } else {
            categories.forEachIndexed { index, (channel, title, enabled) ->
                if (index > 0) HairlineDivider()
                GroupedRow(
                    title,
                    value = if (enabled) "On" else "Off",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, channel))
                    }
                )
            }
        }
    }
}
