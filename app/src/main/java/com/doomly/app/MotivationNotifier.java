package com.doomly.app;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;

/** Emits at most one celebration per milestone and never before notification permission. */
public final class MotivationNotifier {
    private static final String PREFS = "doomly_motivation";
    private static final String KEY_DAY = "day";
    private static final String KEY_MILESTONE = "milestone";

    private MotivationNotifier() {}

    public static void celebrateIfNeeded(Context context, DoomStatsStore.Snapshot snap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        String today = LocalDate.now().toString();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int previous = today.equals(prefs.getString(KEY_DAY, ""))
                ? prefs.getInt(KEY_MILESTONE, 0) : 0;
        int step = Math.max(25, Math.min(250, Math.max(1, snap.dailyTarget) / 10));
        int reached = snap.todayReels >= snap.dailyTarget
                ? snap.dailyTarget : (snap.todayReels / step) * step;
        if (reached <= 0 || reached <= previous) return;

        prefs.edit().putString(KEY_DAY, today).putInt(KEY_MILESTONE, reached).apply();
        String body = snap.todayReels >= snap.dailyTarget
                ? "Daily goal complete! Keep going and set a new best."
                : reached + " Reels today. Next stop: "
                    + MotivationEngine.nextMilestone(snap.todayReels, snap.dailyTarget) + ".";
        DoomlyNotifications.show(context, "Milestone reached", body);
    }
}
