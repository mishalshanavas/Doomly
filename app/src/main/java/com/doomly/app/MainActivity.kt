package com.doomly.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.navigation.DoomlyNav
import com.doomly.app.ui.theme.DoomlyTheme

class MainActivity : ComponentActivity() {

    private var resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DoomlyTheme {
                DoomlyNav(resumeCount = resumeCount.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AuthRepository.getInstance().handleDeepLink(this, intent.data)
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++

        // Async token refresh — runs on background thread, no crash
        AuthRepository.getInstance().refreshTokenIfNeeded(this)

        val intent = intent
        if (intent != null && intent.data != null &&
            AuthRepository.getInstance().handleDeepLink(this, intent.data)
        ) {
            setIntent(Intent(this, MainActivity::class.java))
        }

        if (isAccessibilityServiceEnabled()) {
            ContextCompat.startForegroundService(this, Intent(this, DoomlyForegroundService::class.java))
        }

        CloudSyncScheduler.scheduleSync(this)
    }

    // ── Public helpers ──────────────────────────────────────────────────

    fun isIgnoringBatteryOptimizations(): Boolean {
        return DeviceReadiness.batteryOptimizationDisabled(this)
    }

    fun requestIgnoreBatteryOptimizations() {
        DeviceReadiness.requestBatteryExemption(this)
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        return DeviceReadiness.accessibilityEnabled(this)
    }

    fun openInstagram() {
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage("com.instagram.android")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (launch.resolveActivity(packageManager) != null) {
            startActivity(launch)
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
        if (launchIntent != null) {
            startActivity(launchIntent)
            return
        }

        val reelsWeb = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/"))
        reelsWeb.setPackage("com.instagram.android")
        if (reelsWeb.resolveActivity(packageManager) != null) {
            startActivity(reelsWeb)
            return
        }

        val browserReels = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/"))
        if (browserReels.resolveActivity(packageManager) != null) {
            startActivity(browserReels)
            return
        }

        val playStore = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.instagram.android"))
        if (playStore.resolveActivity(packageManager) != null) {
            startActivity(playStore)
            return
        }

        Toast.makeText(this, "Instagram is not installed.", Toast.LENGTH_LONG).show()
    }
}
