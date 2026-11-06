package com.doomly.app.ui.screens

import com.doomly.app.ui.theme.Void
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
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
@OptIn(ExperimentalMaterial3Api::class)
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
    var loginError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var authenticated by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(false) }
    var article by rememberSaveable { mutableStateOf("Quantum_entanglement") }
    var visible by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    var reels by rememberSaveable { mutableStateOf(true) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, browser) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    browser?.evaluateJavascript("document.querySelectorAll('video').forEach(v => v.pause())", null)
                    browser?.onPause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> browser?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) { onDispose { browser?.destroy(); browser = null } }

    Column(Modifier.fillMaxSize().background(Void)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { controls = !controls }) { Icon(Icons.Default.MoreVert, contentDescription = "Research controls") }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close research") }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .nestedScroll(remember(authenticated, browser) {
                    object : NestedScrollConnection {
                        var distance = 0f
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (source == NestedScrollSource.UserInput) distance += available.y
                            return Offset.Zero
                        }
                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (authenticated && kotlin.math.abs(distance) > 80) {
                                val direction = if (distance < 0) 1 else -1
                                browser?.evaluateJavascript("window.scrollBy({top: window.innerHeight * $direction, behavior: 'smooth'})", null)
                            }
                            distance = 0f
                            return Velocity.Zero
                        }
                    }
                })
                .pointerInput(authenticated, browser) {
                    detectTapGestures(onLongPress = {
                        if (authenticated) browser?.evaluateJavascript("document.querySelectorAll('video').forEach(v => { v.dataset.doomlyPlaying = String(!v.paused); v.pause(); })", null)
                    }, onPress = {
                        tryAwaitRelease()
                        if (authenticated) browser?.evaluateJavascript("document.querySelectorAll('video').forEach(v => { if(v.dataset.doomlyPlaying === 'true') {v.play().catch(()=>{}); delete v.dataset.doomlyPlaying;} })", null)
                    })
                }.padding(24.dp)) {
                Text(article.replace('_', ' '), style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.clickable { controls = !controls })
                Spacer(Modifier.height(12.dp))
                Text("Overview", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(32.dp))
                val paragraphs = when(article) {
                    "Artificial_neural_network" -> listOf(
                        "Learning from examples", "A neural network is a mathematical model made of connected units. Each connection carries a weight. During training, those weights change to reduce the difference between the model’s output and a target.",
                        "Layers and representations", "Hidden layers transform an input into intermediate representations. Different architectures suit different kinds of data, including images, sequences and graphs.",
                        "Generalization", "A low training error does not guarantee useful predictions on new data. Evaluation on separate data helps reveal overfitting. Results also depend on the quality and coverage of the training examples.")
                    "General_relativity" -> listOf(
                        "Gravity and geometry", "General relativity describes gravity through the geometry of spacetime. Matter and energy influence curvature, while freely falling objects follow paths determined by that geometry.",
                        "Local observations", "In a sufficiently small region, free fall resembles motion without gravity. This relationship between acceleration and gravity guided Einstein’s development of the theory.",
                        "Observable effects", "The theory predicts gravitational time dilation, the bending of light and gravitational waves. Its predictions have been tested through astronomical observations and precision measurements.")
                    else -> listOf(
                        "A shared quantum state", "Entanglement occurs when a composite quantum system cannot be described as independent states for each of its parts. Predictions must instead refer to the joint state.",
                        "Correlations and measurement", "Measurements of entangled systems can exhibit correlations that differ from those allowed by local hidden-variable models. Bell inequalities provide a way to test those differences.",
                        "Information has limits", "Entanglement does not allow controllable messages to travel faster than light. Comparing measurement outcomes still requires an ordinary communication channel.")
                }
                paragraphs.chunked(2).forEach { section ->
                    Text(section[0], style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(section[1], style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(40.dp))
                }
                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/$article"))) }) { Text("Read source · Wikipedia") }
                Spacer(Modifier.height(200.dp))
            }
            if (visible) {
                val width = if (expanded) maxWidth else minOf(112.dp, maxWidth)
                val height = if (expanded) maxHeight else minOf(200.dp, maxHeight)
                val density = LocalDensity.current
                val limitX = with(density) { (maxWidth - width).toPx() }
                val limitY = with(density) { (maxHeight - height).toPx() }
                Box(
                    modifier = Modifier.offset { IntOffset((limitX + x).coerceIn(0f, limitX).roundToInt(), (limitY + y).coerceIn(0f, limitY).roundToInt()) }.size(width, height)
                        .pointerInput(limitX, limitY) {
                            detectDragGesturesAfterLongPress { change, delta ->
                                change.consume()
                                x = (x + delta.x).coerceIn(-limitX, 0f)
                                y = (y + delta.y).coerceIn(-limitY, 0f)
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
                                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                        loading = true
                                        loginError = null
                                    }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                        if (request.isForMainFrame) {
                                            loading = false
                                            loginError = "Instagram could not load. Check your connection and retry."
                                        }
                                    }
                                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                        if (request.isForMainFrame) {
                                            loading = false
                                            loginError = "Instagram returned HTTP ${response.statusCode}. Try again later."
                                        }
                                    }
                                    override fun onPageFinished(view: WebView, url: String) {
                                        loading = false
                                        if (Uri.parse(url).host == "www.instagram.com") {
                                            // Some Instagram login layouts collapse in older WebView versions.
                                            // Repair only zero-height ancestors of a visible login input.
                                            view.evaluateJavascript("""
                                                (() => {
                                                    const repair = () => {
                                                        const input = document.querySelector('input[type="password"]');
                                                        if (!input) return;
                                                        let node = input.parentElement;
                                                        while (node) {
                                                            if (node.getBoundingClientRect().height === 0 && getComputedStyle(node).display !== 'none') {
                                                                node.style.setProperty('min-height', window.innerHeight + 'px', 'important');
                                                            }
                                                            node = node.parentElement;
                                                        }
                                                    };
                                                    repair();
                                                    if (!window.doomlyLoginRepair) {
                                                        window.doomlyLoginRepair = new MutationObserver(repair);
                                                        window.doomlyLoginRepair.observe(document.body, {childList:true, subtree:true});
                                                    }
                                                })();
                                            """.trimIndent(), null)
                                        }
                                        if (Uri.parse(url).host?.endsWith("instagram.com") == true) {
                                            view.evaluateJavascript("document.querySelector('a[href=\"/direct/inbox/\"]') !== null") { result ->
                                                if (result == "true" && !authenticated) { authenticated = true; expanded = false; view.loadUrl("https://www.instagram.com/reels/") }
                                            }
                                        }
                                    }
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val scheme = request.url.scheme
                                        if (scheme == "https" || scheme == "http") return false
                                        Toast.makeText(context, "Use Open Instagram app for app-only links", Toast.LENGTH_SHORT).show()
                                        return true
                                    }
                                }
                                browser = this
                                loadUrl("https://www.instagram.com/")
                            }
                        }, modifier = Modifier.fillMaxSize(), onRelease = {
                            if (browser === it) browser = null
                            it.stopLoading()
                            it.destroy()
                        })
                    if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    loginError?.let { message ->
                        Column(Modifier.fillMaxSize().background(Void).padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Instagram unavailable", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            Text(message, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { loginError = null; loading = true; browser?.loadUrl("https://www.instagram.com/") }) { Text("Retry") }
                        }
                    }
                }
            }
        }
    }
    if (controls) ModalBottomSheet(onDismissRequest = { controls = false }) {
        Column(Modifier.padding(24.dp)) {
            Text("Research", style = MaterialTheme.typography.titleLarge)
            listOf("Quantum" to "Quantum_entanglement", "Neural networks" to "Artificial_neural_network", "Relativity" to "General_relativity").forEach { (label, page) ->
                TextButton(onClick = { article = page; controls = false }) { Text(label) }
            }
            TextButton(onClick = { visible = true; expanded = true; controls = false }) { Text("Instagram") }
            if (authenticated) {
                TextButton(onClick = { visible = !visible; controls = false }) { Text(if (visible) "Hide Instagram" else "Show Instagram") }
                TextButton(onClick = { expanded = !expanded; controls = false }) { Text(if (expanded) "Small window" else "Expand window") }
                TextButton(onClick = { browser?.loadUrl("https://www.instagram.com/reels/"); controls = false }) { Text("Reels") }
                TextButton(onClick = { browser?.loadUrl("https://www.instagram.com/"); controls = false }) { Text("Feed") }
            }
        }
    }

}
