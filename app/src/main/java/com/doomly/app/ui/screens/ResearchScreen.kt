package com.doomly.app.ui.screens

import com.doomly.app.ui.theme.Void
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/** In-app floating browser; no overlay permission or access to Instagram credentials. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ResearchScreen(onClose: () -> Unit = {}) {
    val context = LocalContext.current
    DisposableEffect(context) {
        var base: Context = context
        while (base is ContextWrapper && base !is Activity) base = base.baseContext
        val window = (base as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            previousBehavior?.let { controller?.systemBarsBehavior = it }
        }
    }
    var controls by remember { mutableStateOf(false) }
    var article by rememberSaveable { mutableStateOf("Quantum_entanglement") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var reels by rememberSaveable { mutableStateOf(true) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) { onDispose { browser?.destroy(); browser = null } }

    Column(Modifier.fillMaxSize().background(Void)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { controls = !controls }) { Icon(Icons.Default.MoreVert, contentDescription = "Research controls") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close research") }
        }
        if (controls) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide window" else "Open Instagram") }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Shrink" else "Expand") }
        }
        Row {
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/")).setPackage("com.instagram.android"))
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, "Instagram app is not installed", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Open Instagram app") }
            TextButton(onClick = { visible = true; expanded = true; browser?.loadUrl("https://www.instagram.com/accounts/login/") }) { Text("Web login") }
        }
        Row {
            listOf("Quantum" to "Quantum_entanglement", "Neural nets" to "Artificial_neural_network", "Relativity" to "General_relativity").forEach { (label, page) ->
                TextButton(onClick = { article = page }) { Text(label) }
            }
        }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    tag = article
                    loadUrl("https://en.wikipedia.org/wiki/$article")
                }
            }, update = { if (it.tag != article) { it.tag = article; it.loadUrl("https://en.wikipedia.org/wiki/$article") } }, modifier = Modifier.fillMaxSize(), onRelease = { it.destroy() })
            if (visible) {
                val width = if (expanded) maxWidth else minOf(280.dp, maxWidth)
                val height = if (expanded) maxHeight else minOf(400.dp, maxHeight)
                val density = LocalDensity.current
                val limitX = with(density) { (maxWidth - width).toPx() }
                val limitY = with(density) { (maxHeight - height).toPx() }
                Box(
                    modifier = Modifier.offset { IntOffset(x.coerceIn(0f, limitX).roundToInt(), y.coerceIn(0f, limitY).roundToInt()) }.size(width, height)
                        .pointerInput(limitX, limitY) {
                            detectDragGesturesAfterLongPress { change, delta ->
                                change.consume()
                                x = (x + delta.x).coerceIn(0f, limitX)
                                y = (y + delta.y).coerceIn(0f, limitY)
                            }
                        }
                ) {
                        AndroidView(factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                // Use the installed WebView's browser version, without its embedded-app marker.
                                settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mediaPlaybackRequiresUserGesture = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val scheme = request.url.scheme
                                        if (scheme == "https" || scheme == "http") return false
                                        Toast.makeText(context, "Use Open Instagram app for app-only links", Toast.LENGTH_SHORT).show()
                                        return true
                                    }
                                }
                                browser = this
                                loadUrl(if (reels) "https://www.instagram.com/reels/" else "https://www.instagram.com/")
                            }
                        }, modifier = Modifier.fillMaxSize(), onRelease = {
                            if (browser === it) browser = null
                            it.stopLoading()
                            it.destroy()
                        })
                }
            }
        }
    }
}
