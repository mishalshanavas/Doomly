package com.doomly.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.doomly.app.InstagramWebPolicy
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Read: article fills the screen. Instagram is a floating PiP window
 * anchored bottom-end. Dragging its toolbar moves it; touches inside the
 * WebView scroll reels only — the article never scrolls the PiP and vice versa.
 *
 * Login fixes (previously broken):
 *  - third-party cookies were blocked, so Instagram's login POST never stuck
 *  - cookies were never flushed, so the session was lost on destroy
 *  - auth was detected by a one-shot DOM query on page load; Instagram is an
 *    SPA and login completes without a reload, so it never fired
 *  - mediaPlaybackRequiresUserGesture blocked reel autoplay
 *  - intent:// and other app schemes used by login/2FA were silently dropped
 * Now: session-cookie polling (sessionid/ds_user_id), flushed persistent
 * cookies, autoplay allowed, external schemes handed off with fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ResearchScreen(onClose: () -> Unit = {}) {
    val context = LocalContext.current
    var loginError by remember { mutableStateOf<String?>(null) }
    // Sticky: Chromium fires onPageStarted for its internal error page after
    // onReceivedHttpError, which would otherwise clear the error state.
    var httpError by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var authenticated by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(false) }
    var article by rememberSaveable { mutableStateOf("Quantum_entanglement") }
    var visible by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var browser by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    fun checkAuth(view: WebView?) {
        val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com/")
        if (!authenticated && InstagramWebPolicy.isAuthenticatedCookie(cookies)) {
            authenticated = true
            CookieManager.getInstance().flush()
            val url = view?.url.orEmpty()
            if (InstagramWebPolicy.isLoginUrl(url) || url.trimEnd('/') == "https://www.instagram.com") {
                view?.loadUrl("https://www.instagram.com/reels/")
            }
        }
    }
    // SPA login completes without onPageFinished — poll the session cookie.
    LaunchedEffect(visible) {
        while (visible) { checkAuth(browser); delay(2_000) }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, browser) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    browser?.evaluateJavascript("document.querySelectorAll('video').forEach(v => v.pause())", null)
                    browser?.onPause(); CookieManager.getInstance().flush()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> { browser?.onResume(); checkAuth(browser) }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(Unit) { onDispose { CookieManager.getInstance().flush() } }
    BackHandler(enabled = canGoBack && visible) { browser?.goBack() }

    var pipX by remember { mutableFloatStateOf(0f) }
    var pipY by remember { mutableFloatStateOf(0f) }

    Column(Modifier.fillMaxSize().background(Ground)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(article.replace('_', ' '), style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
            TextButton(onClick = { controls = !controls }) { Text("Index", color = Ink) }
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Filled.PictureInPictureAlt else Icons.Filled.PictureInPicture, contentDescription = if (visible) "Hide Instagram" else "Show Instagram", tint = Ink)
            }
        }
        HorizontalDivider(color = Hairline, thickness = 0.5.dp)
        BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding()) {
            val paragraphs = when (article) {
                "Artificial_neural_network" -> listOf(
                    "Learning from examples" to "A neural network is a mathematical model made of connected units. Each connection carries a weight. During training, those weights change to reduce the difference between the model's output and a target.",
                    "Layers and representations" to "Hidden layers transform an input into intermediate representations. Different architectures suit different kinds of data, including images, sequences and graphs.",
                    "Generalization" to "A low training error does not guarantee useful predictions on new data. Evaluation on separate data helps reveal overfitting."
                )
                "General_relativity" -> listOf(
                    "Gravity and geometry" to "General relativity describes gravity through the geometry of spacetime. Matter and energy influence curvature, while freely falling objects follow paths determined by that geometry.",
                    "Local observations" to "In a sufficiently small region, free fall resembles motion without gravity. This relationship between acceleration and gravity guided Einstein's development of the theory.",
                    "Observable effects" to "The theory predicts gravitational time dilation, the bending of light and gravitational waves. Its predictions have been tested through astronomical observations and precision measurements."
                )
                else -> listOf(
                    "A shared quantum state" to "Entanglement occurs when a composite quantum system cannot be described as independent states for each of its parts. Predictions must instead refer to the joint state.",
                    "Correlations and measurement" to "Measurements of entangled systems can exhibit correlations that differ from those allowed by local hidden-variable models. Bell inequalities provide a way to test those differences.",
                    "Information has limits" to "Entanglement does not allow controllable messages to travel faster than light. Comparing measurement outcomes still requires an ordinary communication channel."
                )
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(paragraphs) { (heading, body) ->
                    Text(heading, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = Ink)
                    Spacer(Modifier.height(20.dp))
                }
                item {
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/$article"))) }) { Text("Wikipedia", color = Ink) }
                    Spacer(Modifier.height(if (visible) 340.dp else 24.dp))
                }
            }

            if (visible) {
                val pipW = if (expanded) minOf(300.dp, maxWidth - 24.dp) else minOf(168.dp, maxWidth - 24.dp)
                val pipH = if (expanded) minOf(520.dp, maxHeight - 24.dp) else minOf(300.dp, maxHeight - 24.dp)
                val density = LocalDensity.current
                val limitX = with(density) { (maxWidth - pipW - 12.dp).toPx().coerceAtLeast(0f) }
                val limitY = with(density) { (maxHeight - pipH - 12.dp).toPx().coerceAtLeast(0f) }
                // Default anchor: bottom-end. pipX/pipY are offsets left/up from there.
                Column(
                    Modifier.offset { IntOffset((limitX - pipX).coerceIn(0f, limitX).roundToInt(), (limitY - pipY).coerceIn(0f, limitY).roundToInt()) }
                        .size(pipW, pipH).shadow(4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Surface)
                ) {
                    // Drag handle — only this bar moves the window, so reel swipes never drag it.
                    Row(
                        Modifier.fillMaxWidth().background(Field)
                            .pointerInput(limitX, limitY) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    pipX = (pipX - drag.x).coerceIn(0f, limitX)
                                    pipY = (pipY - drag.y).coerceIn(0f, limitY)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DragHandle, contentDescription = "Move", tint = Smoke, modifier = Modifier.size(18.dp))
                        Text(if (authenticated) "Reels" else "Instagram", style = MaterialTheme.typography.labelMedium, color = Ink, modifier = Modifier.weight(1f))
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                            Icon(if (expanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull, contentDescription = if (expanded) "Shrink" else "Expand", tint = Ink, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { visible = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Hide", tint = Ink, modifier = Modifier.size(18.dp))
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress; loading = newProgress < 100 }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) { loading = true; if (httpError == null) loginError = null }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                        if (request.isForMainFrame) { loading = false; httpError = -1; loginError = "Couldn't load. Check connection." }
                                    }
                                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                        if (request.isForMainFrame && response.statusCode >= 400) { httpError = response.statusCode }
                                    }
                                    override fun onPageFinished(view: WebView, url: String?) {
                                        loading = false; canGoBack = view.canGoBack()
                                        httpError?.let { code -> loginError = InstagramWebPolicy.errorMessage(code) }
                                        CookieManager.getInstance().flush()
                                        checkAuth(view)
                                    }
                                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) { canGoBack = view.canGoBack(); checkAuth(view) }
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val uri = request.url
                                        if (!InstagramWebPolicy.isExternalScheme(uri.scheme)) {
                                            // Non-Instagram web links open in the browser, not the PiP.
                                            if ((uri.scheme == "https" || uri.scheme == "http") && !InstagramWebPolicy.isInstagramHost(uri.host)) {
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                                                return true
                                            }
                                            return false
                                        }
                                        // intent://, fb://, market:// etc. used by login / 2FA / app links.
                                        try {
                                            val intent = if (uri.scheme == "intent") Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) else Intent(Intent.ACTION_VIEW, uri)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val fallback = request.url.getQueryParameter("browser_fallback_url")
                                            if (fallback != null) view.loadUrl(fallback)
                                            else Toast.makeText(context, "Open in Instagram app", Toast.LENGTH_SHORT).show()
                                        }
                                        return true
                                    }
                                }
                                browser = this
                                // Resume the persisted session instead of restarting login every visit.
                                val cookies = CookieManager.getInstance().getCookie("https://www.instagram.com/")
                                if (InstagramWebPolicy.isAuthenticatedCookie(cookies)) {
                                    authenticated = true; loadUrl("https://www.instagram.com/reels/")
                                } else loadUrl("https://www.instagram.com/accounts/login/")
                            }
                        }, modifier = Modifier.fillMaxSize(), onRelease = {
                            if (browser === it) browser = null
                            CookieManager.getInstance().flush()
                            it.stopLoading(); it.destroy()
                        })
                        if (loading) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Accent, trackColor = Field)
                        loginError?.let { message ->
                            Column(Modifier.fillMaxSize().background(Surface).padding(16.dp), verticalArrangement = Arrangement.Center) {
                                Text("Couldn't load", style = MaterialTheme.typography.titleLarge, color = Ink)
                                Spacer(Modifier.height(6.dp))
                                Text(message, style = MaterialTheme.typography.bodySmall, color = Smoke)
                                TextButton(onClick = { httpError = null; loginError = null; loading = true; browser?.reload() }) { Text("Retry", color = Ink) }
                                TextButton(onClick = {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/"))) } catch (_: Exception) {}
                                }) { Text("Open in browser", color = Ink) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (controls) ModalBottomSheet(onDismissRequest = { controls = false }, shape = DoomlyShapes.extraLarge, containerColor = BodyCard, dragHandle = { BottomSheetDefaults.DragHandle(color = BodyFill) }) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Index", style = MaterialTheme.typography.headlineSmall, color = BodyInk)
            Spacer(Modifier.height(8.dp))
            listOf("Quantum" to "Quantum_entanglement", "Neural networks" to "Artificial_neural_network", "Relativity" to "General_relativity").forEach { (label, page) ->
                GroupedRow(label, value = if (article == page) "✓" else null, showChevron = false, onClick = { article = page; controls = false })
                HairlineDivider()
            }
            GroupedRow("Instagram", value = if (visible) "Shown" else "Hidden", showChevron = false, onClick = { visible = !visible; controls = false })
            if (authenticated) {
                HairlineDivider()
                GroupedRow("Reels", onClick = { browser?.loadUrl("https://www.instagram.com/reels/"); visible = true; controls = false })
                HairlineDivider()
                GroupedRow("Feed", onClick = { browser?.loadUrl("https://www.instagram.com/"); visible = true; controls = false })
                HairlineDivider()
                GroupedRow("Sign out", onClick = {
                    CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
                    WebStorage.getInstance().deleteAllData()
                    authenticated = false; browser?.loadUrl("https://www.instagram.com/accounts/login/"); controls = false
                })
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
