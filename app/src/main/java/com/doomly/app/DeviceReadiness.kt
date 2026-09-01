package com.doomly.app

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object DeviceReadiness {
    fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = ComponentName(context, DoomlyAccessibilityService::class.java)
        return enabled.split(':').mapNotNull(ComponentName::unflattenFromString).any { it == expected }
    }

    fun batteryOptimizationDisabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val manager = context.getSystemService(PowerManager::class.java) ?: return true
        return manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryExemption(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || batteryOptimizationDisabled(activity)) return
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + activity.packageName))
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            activity.startActivity(if (direct.resolveActivity(activity.packageManager) != null) direct else fallback)
        } catch (_: Exception) {
            activity.startActivity(fallback)
        }
    }
}
