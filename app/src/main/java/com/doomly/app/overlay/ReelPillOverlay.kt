package com.doomly.app.overlay

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.view.Display
import android.util.Log
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import com.doomly.app.R
import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.MotionEvent
import android.view.HapticFeedbackConstants
import android.view.animation.OvershootInterpolator

/** A short, opt-in overlay tied to a confirmed Reel, never a full-screen window. */
class ReelPillOverlay(private val context: Context, private val isReelsForeground: () -> Boolean) {
    private val windowContext by lazy {
        if (Build.VERSION.SDK_INT >= 30) {
            // A Service is not attached to a display. Associate one before creating UI.
            val display = context.getSystemService(DisplayManager::class.java)
                .getDisplay(Display.DEFAULT_DISPLAY)
                ?: throw IllegalStateException("No display available for the Reel pill")
            context.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } else context
    }
    private val windows by lazy { windowContext.getSystemService(WindowManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = preferences(context)
    private var pill: View? = null
    private var snoozed = false
    private var lastCount = 0
    private var lastShown = -30_000L
    fun beginSession() { hide(); snoozed = false; lastCount = 0; lastShown = -30_000L }
    fun preview() { safely { showMilestone(30, true) } }
    private val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") handler.post { safely { if (!enabled(context)) hide() } }
        if (key == "reduce_motion") handler.post { hide() }
    }
    init { prefs.registerOnSharedPreferenceChangeListener(changed) }

    fun showIfMilestone(reels: Int) {
        safely { showMilestone(reels) }
    }

    // Covers window creation as well as callbacks dispatched after addView returns.
    private inline fun safely(action: () -> Unit) {
        try { action() } catch (error: RuntimeException) {
            Log.w("ReelPillOverlay", "Could not display Reel celebration", error)
            hide()
        }
    }

    private fun showMilestone(reels: Int, preview: Boolean = false) {
        if (reels <= 0 || reels % 10 != 0 || !enabled(context)) return
        if (!preview && (snoozed || reels <= lastCount || SystemClock.elapsedRealtime() - lastShown < 30_000L)) return
        if (!preview && !isReelsForeground()) return
        val motion = ValueAnimator.areAnimatorsEnabled() && !prefs.getBoolean("reduce_motion", false)
        hide()
        val width = screenWidth()
        val view = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            minimumHeight = dp(40)
            background = GradientDrawable().apply { setColor(Color.rgb(28, 28, 30)); cornerRadius = 0f }
            alpha = 0f
        }
        view.addView(View(windowContext).apply {
            setBackgroundColor(Color.rgb(10, 207, 112))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(10) })
        val message = "$reels Reels · ${appreciation(reels)}"
        view.addView(TextView(windowContext).apply {
            text = message
            textSize = 13f
            setTextColor(Color.rgb(244, 244, 246))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            fontFeatureSettings = "tnum"
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = (width - dp(84)).coerceAtLeast(dp(80))
        })
        view.contentDescription = "$message. Tap or swipe to dismiss. Hold to snooze this session."
        view.setOnClickListener { hide() }
        view.setOnLongClickListener { if (!preview) snoozed = true; hide(); true }
        var downX = 0f
        var downY = 0f
        view.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; false }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawX - downX) > dp(32) || kotlin.math.abs(event.rawY - downY) > dp(32)) {
                        target.cancelLongPress(); hide(); true
                    } else false
                }
                else -> false
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = width / 2
            y = 0
            if (Build.VERSION.SDK_INT >= 30) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            else if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        fun position(insets: WindowInsetsCompat?) {
            if (pill !== view || view.width == 0) return
            val displayInsets = if (Build.VERSION.SDK_INT >= 30) {
                WindowInsetsCompat.toWindowInsetsCompat(windows.currentWindowMetrics.windowInsets)
            } else insets
            val cutout = displayInsets?.displayCutout
            val top = displayInsets?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())?.top ?: dp(28)
            val camera = cutout?.boundingRects?.firstOrNull { it.top < (cutout?.safeInsetTop ?: top) && it.width() > 0 }
            val availableWidth = screenWidth()
            val x = (((if (prefs.getBoolean("camera_aligned", true)) camera?.centerX() else null) ?: availableWidth / 2) - view.width / 2)
                .coerceIn(dp(12), (availableWidth - view.width - dp(12)).coerceAtLeast(dp(12)))
            val y = maxOf(top, dp(24), cutout?.safeInsetTop ?: 0, camera?.bottom ?: 0) + dp(8 + prefs.getInt("offset", 0).coerceIn(0, 120))
            if (params.x == x && params.y == y) return
            params.x = x
            params.y = y
            try { windows.updateViewLayout(view, params) } catch (_: RuntimeException) { hide() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> safely { position(insets) }; insets }
        pill = view
        try {
            windows.addView(view, params)
            if (!preview) { lastCount = reels; lastShown = SystemClock.elapsedRealtime() }
            view.doOnLayout {
                safely {
                    position(ViewCompat.getRootWindowInsets(view))
                    if (pill === view) {
                        if (motion) {
                            view.pivotX = view.width / 2f
                            view.scaleX = .15f; view.scaleY = .6f
                            view.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                .setInterpolator(OvershootInterpolator(.65f)).setDuration(280).start()
                        } else view.alpha = 1f
                        if (prefs.getBoolean("haptics", false)) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
            }
            ViewCompat.requestApplyInsets(view)
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (pill !== view) return
                    safely {
                        if (!enabled(context) || (!preview && !isReelsForeground())) hide()
                        else handler.postDelayed(this, 250L)
                    }
                }
            }, 250L)
            handler.postDelayed({
                safely {
                    if (pill === view) view.animate().alpha(0f).setDuration(if (motion) 140 else 0).withEndAction { if (pill === view) hide() }.start()
                }
            }, 2_000L)
        } catch (_: RuntimeException) { hide() }
    }

    fun hide() {
        val current = pill
        pill = null
        handler.removeCallbacksAndMessages(null)
        if (current != null) {
            try { current.animate().cancel() } catch (_: RuntimeException) { /* Already detached. */ }
            try { windows.removeViewImmediate(current) } catch (_: RuntimeException) { /* Already detached. */ }
        }
    }

    fun close() { hide(); prefs.unregisterOnSharedPreferenceChangeListener(changed) }
    private fun dp(value: Int) = (value * windowContext.resources.displayMetrics.density).toInt()
    @Suppress("DEPRECATION")
    private fun screenWidth(): Int = if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.bounds.width()
        else windowContext.resources.displayMetrics.widthPixels

    companion object {
        fun preferences(context: Context): SharedPreferences = context.getSharedPreferences("doomly_reel_pill", Context.MODE_PRIVATE)
        fun enabled(context: Context): Boolean = preferences(context).getBoolean("enabled", false) && Settings.canDrawOverlays(context)
        private fun appreciation(reels: Int): String = when {
            reels >= 100 -> "Doom, expertly done."
            reels >= 50 -> "One more masterpiece?"
            reels == 40 -> "Your thumb has talent."
            reels == 30 -> "Keep the Reels rolling."
            reels == 20 -> "Excellent scrolling."
            else -> "A promising scroll."
        }
    }
}
