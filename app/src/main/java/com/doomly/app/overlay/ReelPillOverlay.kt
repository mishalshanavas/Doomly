package com.doomly.app.overlay

import android.content.Context
import android.content.SharedPreferences
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
import java.time.LocalDate

/** A short, opt-in overlay tied to a confirmed Reel, never a full-screen window. */
class ReelPillOverlay(private val context: Context, private val isReelsForeground: () -> Boolean) {
    private val windowContext by lazy {
        if (Build.VERSION.SDK_INT >= 30) context.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        else context
    }
    private val windows by lazy { windowContext.getSystemService(WindowManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = preferences(context)
    private var pill: View? = null
    private val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") handler.post { if (!enabled(context)) hide() }
    }
    init { prefs.registerOnSharedPreferenceChangeListener(changed) }

    fun showIfMilestone(reels: Int) {
        if (reels <= 0 || reels % 10 != 0 || !enabled(context)) return
        val day = LocalDate.now().toString()
        if (prefs.getString("last_day", "") == day && prefs.getInt("last_count", 0) >= reels) return
        if (!isReelsForeground()) return
        hide()
        val width = screenWidth()
        val view = LinearLayout(windowContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            minimumHeight = dp(40)
            background = GradientDrawable().apply { setColor(Color.rgb(23, 23, 23)); cornerRadius = dp(40).toFloat() }
            alpha = 0f
        }
        view.addView(ImageView(windowContext).apply {
            setImageResource(R.drawable.ic_stat_doomly)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(8) })
        val message = "$reels Reels · ${appreciation(reels)}"
        view.addView(TextView(windowContext).apply {
            text = message
            textSize = 13f
            setTextColor(Color.rgb(245, 244, 239))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = (width - dp(84)).coerceAtLeast(dp(80))
        })
        view.contentDescription = "$message. Tap or hold to dismiss."
        view.setOnClickListener { hide() }
        view.setOnLongClickListener { hide(); true }
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
            val x = ((camera?.centerX() ?: availableWidth / 2) - view.width / 2)
                .coerceIn(dp(12), (availableWidth - view.width - dp(12)).coerceAtLeast(dp(12)))
            val y = maxOf(top, dp(24), cutout?.safeInsetTop ?: 0, camera?.bottom ?: 0) + dp(8)
            if (params.x == x && params.y == y) return
            params.x = x
            params.y = y
            try { windows.updateViewLayout(view, params) } catch (_: RuntimeException) { hide() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets -> position(insets); insets }
        pill = view
        try {
            windows.addView(view, params)
            prefs.edit().putString("last_day", day).putInt("last_count", reels).apply()
            view.doOnLayout {
                position(ViewCompat.getRootWindowInsets(view))
                if (pill === view) view.animate().alpha(1f).setDuration(160).start()
            }
            ViewCompat.requestApplyInsets(view)
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (pill !== view) return
                    if (!enabled(context) || !isReelsForeground()) { hide(); return }
                    handler.postDelayed(this, 250L)
                }
            }, 250L)
            handler.postDelayed({
                if (pill === view) view.animate().alpha(0f).setDuration(140).withEndAction { if (pill === view) hide() }.start()
            }, 3_000L)
        } catch (_: RuntimeException) { hide() }
    }

    fun hide() {
        val current = pill
        pill = null
        handler.removeCallbacksAndMessages(null)
        current?.animate()?.cancel()
        if (current != null) try { windows.removeViewImmediate(current) } catch (_: RuntimeException) { /* Already detached. */ }
    }

    fun close() { hide(); prefs.unregisterOnSharedPreferenceChangeListener(changed) }
    private fun dp(value: Int) = (value * windowContext.resources.displayMetrics.density).toInt()
    @Suppress("DEPRECATION")
    private fun screenWidth(): Int = if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.bounds.width()
        else windowContext.resources.displayMetrics.widthPixels

    companion object {
        fun preferences(context: Context): SharedPreferences = context.getSharedPreferences("doomly_reel_pill", Context.MODE_PRIVATE)
        fun enabled(context: Context): Boolean = preferences(context).getBoolean("enabled", false) && Settings.canDrawOverlays(context)
        private fun appreciation(reels: Int): String = when ((reels / 10 - 1) % 6) {
            0 -> "A fine start."
            1 -> "Research noted."
            2 -> "Well observed."
            3 -> "Solid fieldwork."
            4 -> "A respectable detour."
            else -> "Nicely done."
        }
    }
}
