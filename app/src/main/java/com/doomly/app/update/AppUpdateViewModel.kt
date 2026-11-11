package com.doomly.app.update

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.doomly.app.BuildConfig

/** One updater for the whole activity, retained across tab changes and rotation. */
class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = GitHubUpdateManager(application)
    var state: UpdateState by mutableStateOf(UpdateState.Checking)
        private set
    private var generation = 0
    private var busy = false
    private var autoAttemptedTag: String? = null
    private var awaitingInstallPermission = false
    private var lastCheckAt = -1L

    fun onAppOpen() {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES || busy || state is UpdateState.ReadyToInstall) return
        if (!UpdateCheckPolicy.shouldCheck(SystemClock.elapsedRealtime(), lastCheckAt)) return
        checkNow()
    }

    fun checkNow() {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES || busy) return
        lastCheckAt = SystemClock.elapsedRealtime()
        busy = true
        val request = generation
        manager.check { if (request == generation) receive(it) }
    }

    fun changeChannel() {
        if (state is UpdateState.Downloading) return
        generation++
        manager.cancel()
        busy = false
        autoAttemptedTag = null
        awaitingInstallPermission = false
        state = UpdateState.Checking
        checkNow()
    }

    private fun receive(next: UpdateState) {
        state = next
        when (next) {
            is UpdateState.Available -> download(next.release)
            is UpdateState.Checking, is UpdateState.Downloading -> busy = true
            else -> busy = false
        }
    }

    fun download(release: GitHubRelease) {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES || state is UpdateState.Downloading) return
        busy = true
        val request = generation
        manager.download(release) { if (request == generation) receive(it) }
    }

    fun onActivityResumed(activity: Activity) {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES) return
        val ready = state as? UpdateState.ReadyToInstall ?: return
        if (awaitingInstallPermission) {
            awaitingInstallPermission = false
            if (activity.packageManager.canRequestPackageInstalls()) install(activity)
        } else if (autoAttemptedTag != ready.release.tagName) {
            autoAttemptedTag = ready.release.tagName
            install(activity)
        }
    }

    fun install(activity: Activity) {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES) return
        val ready = state as? UpdateState.ReadyToInstall ?: return
        autoAttemptedTag = ready.release.tagName
        val needsPermission = !activity.packageManager.canRequestPackageInstalls()
        val started = manager.install(activity, ready)
        awaitingInstallPermission = started && needsPermission
        if (!started) state = UpdateState.Failed("Android couldn't open the installer. Check again to retry.")
    }

    override fun onCleared() { manager.cancel() }
}
