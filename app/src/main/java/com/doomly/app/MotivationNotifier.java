package com.doomly.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

public final class MotivationNotifier {
    private MotivationNotifier() {}
    public static void celebrateIfNeeded(Context context, DoomStatsStore.Snapshot snap) {
        if (!DoomlyNotifications.enabled(context, DoomlyNotifications.MILESTONES)) return;
        String today = LocalDate.now().toString();
        SharedPreferences prefs = context.getSharedPreferences("doomly_motivation", Context.MODE_PRIVATE);
        boolean sameDay = today.equals(prefs.getString("day", ""));
        int previous = sameDay ? prefs.getInt("milestone", 0) : 0;
        int sent = sameDay ? prefs.getInt("sent", 0) : 0;
        boolean goalNotified = sameDay && prefs.getBoolean("goal_notified", false);
        int reached = MotivationEngine.reachedMilestone(snap.todayReels, snap.dailyTarget);
        boolean newGoal = snap.todayReels >= snap.dailyTarget && !goalNotified;
        if (reached <= 0 || (reached <= previous && !newGoal)) return;
        long now = System.currentTimeMillis();
        if (!NotificationPolicy.allow(now, prefs.getLong("last_sent", 0), sent, newGoal)) return;
        String body = newGoal ? "Daily goal crushed. Elite thumb work."
                : reached + " Reels today. Next milestone: " + MotivationEngine.nextMilestone(snap.todayReels, snap.dailyTarget) + ".";
        if (DoomlyNotifications.showMilestone(context, body)) {
            prefs.edit().putString("day", today).putInt("milestone", reached)
                .putInt("sent", sent + 1).putLong("last_sent", now)
                .putBoolean("goal_notified", goalNotified || newGoal).apply();
        }
    }
}
