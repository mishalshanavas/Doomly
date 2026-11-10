package com.doomly.app

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object DeviceReadiness {
    fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = ComponentName(context, DoomlyAccessibilityService::class.java)
        return enabled.split(':').mapNotNull(ComponentName::unflattenFromString).any { it == expected }
    }
}
