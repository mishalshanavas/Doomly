package com.doomly.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.doomly.app.update.AppUpdateViewModel
import com.doomly.app.update.UpdateState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import com.doomly.app.data.AuthRepository
import com.doomly.app.ui.navigation.DoomlyNav
import com.doomly.app.ui.theme.DoomlyTheme

class MainActivity : ComponentActivity() {

    private val appUpdates by lazy { ViewModelProvider(this)[AppUpdateViewModel::class.java] }
    private var notificationDestination = mutableStateOf<String?>(null)
    private var resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeNotificationIntent(intent)

        setContent {
            val appearance = androidx.compose.runtime.remember { getSharedPreferences("appearance", MODE_PRIVATE) }
            var dark by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(appearance.getBoolean("dark", true)) }
            androidx.compose.runtime.DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "dark") dark = prefs.getBoolean("dark", true)
                }
                appearance.registerOnSharedPreferenceChangeListener(listener)
                onDispose { appearance.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            DoomlyTheme(darkTheme = dark) {
                val updateState = appUpdates.state
                LaunchedEffect(updateState) {
                    if (updateState is UpdateState.ReadyToInstall && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) appUpdates.onActivityResumed(this@MainActivity)
                }
                DoomlyNav(resumeCount = resumeCount.intValue, notificationDestination = notificationDestination.value, onNotificationHandled = { notificationDestination.value = null }, updates = appUpdates, onInstallUpdate = { appUpdates.install(this@MainActivity) })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appUpdates.onAppOpen()
    }

    override fun onPostResume() {
        super.onPostResume()
        appUpdates.onActivityResumed(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNotificationIntent(intent)
        if (AuthRepository.getInstance().handleDeepLink(this, intent.data)) {
            // Consume the callback so onResume cannot process the same tokens twice.
            setIntent(Intent(this, MainActivity::class.java))
        }
    }

    private fun consumeNotificationIntent(incoming: Intent?) {
        notificationDestination.value = incoming?.getStringExtra("notification_destination")
            ?: if (incoming?.hasExtra("google.message_id") == true) "profile" else null
        incoming?.removeExtra("notification_destination")
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

        CloudSyncScheduler.scheduleSync(this)
    }

    // ── Public helpers ──────────────────────────────────────────────────

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
