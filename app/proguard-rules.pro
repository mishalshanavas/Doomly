# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name in release builds
-renamesourcefileattribute SourceFile

# ── OkHttp ──────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── Supabase / org.json ─────────────────────────────────────────────────
-keep class org.json.** { *; }

# ── Data classes (used via reflection / JSON parsing) ───────────────────
-keep class com.doomly.app.DoomStatsStore$Snapshot { *; }
-keep class com.doomly.app.data.LeaderboardRepository$LeaderboardEntry { *; }
-keep class com.doomly.app.data.SupabaseConfig { *; }
-keep class com.doomly.app.data.SupabaseSession { *; }

# ── Custom views ────────────────────────────────────────────────────────
-keep class com.doomly.app.ui.DoomlyMascotView { *; }
-keep class com.doomly.app.ui.DoomlyHeroView { *; }
-keep class com.doomly.app.ui.MascotView { *; }
-keep class com.doomly.app.ui.AnimatedRingView { *; }

# ── Accessibility service ───────────────────────────────────────────────
-keep class com.doomly.app.DoomlyAccessibilityService { *; }

# Strip verbose diagnostics from production builds while preserving warnings and errors.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ── ResultCallback (used across modules) ────────────────────────────────
-keep class com.doomly.app.ResultCallback { *; }
-keep class com.doomly.app.Refreshable { *; }

# ── ViewModels ──────────────────────────────────────────────────────────
-keep class com.doomly.app.TodayViewModel { *; }
-keep class com.doomly.app.LeagueViewModel { *; }
-keep class com.doomly.app.ProfileViewModel { *; }
