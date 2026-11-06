package com.doomly.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
fun ResearchScreen() {
    var article by rememberSaveable { mutableStateOf("Quantum_entanglement") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var reels by rememberSaveable { mutableStateOf(true) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) { onDispose { browser?.destroy(); browser = null } }

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F2E9))) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { visible = !visible }) { Text(if (visible) "Hide window" else "Open Instagram") }
            TextButton(onClick = { browser?.loadUrl(if (reels) "https://www.instagram.com/reels/" else "https://www.instagram.com/") }) { Text("Reload") }
        }
        Row {
            listOf("Quantum" to "Quantum_entanglement", "Neural nets" to "Artificial_neural_network", "Relativity" to "General_relativity").forEach { (label, page) ->
                TextButton(onClick = { article = page }) { Text(label) }
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
                Surface(
                    modifier = Modifier.offset { IntOffset(x.coerceIn(0f, limitX).roundToInt(), y.coerceIn(0f, limitY).roundToInt()) }.size(width, height),
                    shape = MaterialTheme.shapes.medium, shadowElevation = 12.dp
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth().pointerInput(limitX, limitY) {
                            detectDragGestures { change, delta ->
                                change.consume()
                                x = (x + delta.x).coerceIn(0f, limitX)
                                y = (y + delta.y).coerceIn(0f, limitY)
                            }
                        }, horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Shrink" else "Expand") }
                            TextButton(onClick = { visible = false }) { Text("Hide") }
                        }
                        Row {
                            TextButton(onClick = { reels = false; browser?.loadUrl("https://www.instagram.com/") }) { Text("Feed") }
                            TextButton(onClick = { reels = true; browser?.loadUrl("https://www.instagram.com/reels/") }) { Text("Reels") }
                            TextButton(onClick = { browser?.let { if (it.canGoBack()) it.goBack() } }) { Text("Back") }
                        }
                        AndroidView(factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mediaPlaybackRequiresUserGesture = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                webViewClient = WebViewClient()
                                browser = this
                                loadUrl(if (reels) "https://www.instagram.com/reels/" else "https://www.instagram.com/")
                            }
                        }, modifier = Modifier.fillMaxWidth().weight(1f), onRelease = {
                            if (browser === it) browser = null
                            it.stopLoading()
                            it.destroy()
                        })
                    }
                }
            }
        }
    }
}
