package com.doomly.app.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.doomly.app.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    val versionName: String,
    val tagName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long
)

sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: GitHubRelease) : UpdateState
    data class Downloading(val release: GitHubRelease, val percent: Int) : UpdateState
    data class ReadyToInstall(val release: GitHubRelease, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class GitHubUpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private var activeCall: Call? = null

    fun check(onState: (UpdateState) -> Unit) {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES) {
            onState(UpdateState.UpToDate)
            return
        }
        activeCall?.cancel()
        onState(UpdateState.Checking)
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Doomly/${BuildConfig.VERSION_NAME}")
            .build()
        activeCall = client.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled()) post(onState, UpdateState.Failed("Could not check for updates."))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            post(onState, UpdateState.Failed("Update check failed (${response.code})."))
                            return
                        }
                        try {
                            val json = JSONObject(response.body?.string().orEmpty())
                            val release = parseRelease(json)
                            val state = if (UpdateVersionPolicy.isNewer(release.versionName, BuildConfig.VERSION_NAME)) {
                                UpdateState.Available(release)
                            } else {
                                UpdateState.UpToDate
                            }
                            post(onState, state)
                        } catch (_: Exception) {
                            post(onState, UpdateState.Failed("The release information was invalid."))
                        }
                    }
                }
            })
        }
    }

    fun download(release: GitHubRelease, onState: (UpdateState) -> Unit) {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES) return
        activeCall?.cancel()
        onState(UpdateState.Downloading(release, 0))
        val request = Request.Builder()
            .url(release.apkUrl)
            .header("User-Agent", "Doomly/${BuildConfig.VERSION_NAME}")
            .build()
        activeCall = client.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled()) post(onState, UpdateState.Failed("The update download failed."))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            post(onState, UpdateState.Failed("Download failed (${response.code})."))
                            return
                        }
                        val body = response.body ?: run {
                            post(onState, UpdateState.Failed("The update download was empty."))
                            return
                        }
                        val declaredLength = body.contentLength()
                        if (declaredLength <= 0 || declaredLength > MAX_APK_BYTES) {
                            post(onState, UpdateState.Failed("The update has an unexpected size."))
                            return
                        }
                        val directory = File(appContext.cacheDir, "updates")
                        if (!directory.exists() && !directory.mkdirs()) {
                            post(onState, UpdateState.Failed("Could not prepare the update folder."))
                            return
                        }
                        val partial = File(directory, "doomly-update.part")
                        val apk = File(directory, "doomly-update.apk")
                        partial.delete()
                        apk.delete()
                        try {
                            body.byteStream().use { input ->
                                FileOutputStream(partial).use { output ->
                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                    var downloaded = 0L
                                    var lastPercent = -1
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        downloaded += count
                                        if (downloaded > MAX_APK_BYTES) throw IOException("APK too large")
                                        output.write(buffer, 0, count)
                                        val percent = (downloaded * 100L / declaredLength).toInt().coerceIn(0, 100)
                                        if (percent >= lastPercent + 5) {
                                            lastPercent = percent
                                            post(onState, UpdateState.Downloading(release, percent))
                                        }
                                    }
                                    output.fd.sync()
                                }
                            }
                            if (!partial.renameTo(apk) || !verifyDownloadedApk(apk)) {
                                partial.delete()
                                apk.delete()
                                post(onState, UpdateState.Failed("Update verification failed. Nothing was installed."))
                                return
                            }
                            post(onState, UpdateState.ReadyToInstall(release, apk))
                        } catch (_: Exception) {
                            partial.delete()
                            apk.delete()
                            post(onState, UpdateState.Failed("The update could not be saved safely."))
                        }
                    }
                }
            })
        }
    }

    fun install(activity: Activity, ready: UpdateState.ReadyToInstall): Boolean {
        if (!BuildConfig.ENABLE_GITHUB_UPDATES || !verifyDownloadedApk(ready.apk)) return false
        return try {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                )
                true
            } else {
                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.update-files",
                    ready.apk
                )
                val install = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (install.resolveActivity(activity.packageManager) == null) false
                else {
                    activity.startActivity(install)
                    true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    fun cancel() {
        activeCall?.cancel()
    }

    private fun parseRelease(json: JSONObject): GitHubRelease {
        val tag = json.getString("tag_name")
        val version = tag.removePrefix("v")
        val assets = json.getJSONArray("assets")
        var apkUrl = ""
        var apkSize = 0L
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            if (name.startsWith("Doomly-") && name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.getString("browser_download_url")
                apkSize = asset.optLong("size")
                break
            }
        }
        val uri = Uri.parse(apkUrl)
        require(uri.scheme == "https" && uri.host == "github.com")
        require(apkSize in 1..MAX_APK_BYTES)
        require(UpdateVersionPolicy.isValid(version))
        return GitHubRelease(version, tag, json.optString("body"), apkUrl, apkSize)
    }

    @Suppress("DEPRECATION")
    private fun verifyDownloadedApk(apk: File): Boolean {
        if (!apk.isFile || apk.length() !in 1..MAX_APK_BYTES) return false
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val archive = appContext.packageManager.getPackageArchiveInfo(apk.absolutePath, flags) ?: return false
        if (archive.packageName != appContext.packageName) return false
        val installed = appContext.packageManager.getPackageInfo(appContext.packageName, flags)
        if (archive.longVersionCodeCompat() <= installed.longVersionCodeCompat()) return false
        return certificateDigests(archive) == certificateDigests(installed)
    }

    @Suppress("DEPRECATION")
    private fun certificateDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            info.signatures.orEmpty()
        }
        return signatures.mapTo(linkedSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    private fun post(callback: (UpdateState) -> Unit, state: UpdateState) {
        main.post { callback(state) }
    }

    private companion object {
        const val LATEST_RELEASE_API = "https://api.github.com/repos/mishalshanavas/Doomly/releases/latest"
        const val MAX_APK_BYTES = 100L * 1024L * 1024L
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
