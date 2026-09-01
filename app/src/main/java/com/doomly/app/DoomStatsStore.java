package com.doomly.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DoomStatsStore {
    public static final String ACTION_STATS_CHANGED = "com.doomly.app.STATS_CHANGED";
    public static final int DEFAULT_DAILY_TARGET = 2000;

    private static final String PREFS = "doomly_stats";
    private static final String KEY_TODAY = "today";
    private static final String KEY_TODAY_REELS = "today_reels";
    private static final String KEY_TODAY_LIKES = "today_likes";
    private static final String KEY_TOTAL_REELS = "total_reels";
    private static final String KEY_TOTAL_LIKES = "total_likes";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_ACTIVE_DAY = "last_active_day";
    private static final String KEY_DAILY_TARGET = "daily_target";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_DEBUG_LINE = "debug_line";
    private static final String KEY_DEBUG_UPDATED_AT = "debug_updated_at";
    private static final String KEY_DAILY_HISTORY = "daily_history";
    private static final int MAX_HISTORY_DAYS = 30;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private DoomStatsStore() {
    }

    public static Snapshot snapshot(Context context) {
        SharedPreferences prefs = prefs(context);
        rollDayIfNeeded(prefs);
        int todayReels = prefs.getInt(KEY_TODAY_REELS, 0);
        int todayLikes = prefs.getInt(KEY_TODAY_LIKES, 0);
        int totalReels = prefs.getInt(KEY_TOTAL_REELS, 0);
        int totalLikes = prefs.getInt(KEY_TOTAL_LIKES, 0);
        int streak = prefs.getInt(KEY_STREAK, 0);
        int dailyTarget = prefs.getInt(KEY_DAILY_TARGET, DEFAULT_DAILY_TARGET);
        String displayName = prefs.getString(KEY_DISPLAY_NAME, "");
        return new Snapshot(
                todayReels,
                todayLikes,
                totalReels,
                totalLikes,
                streak,
                todayLikes,
                dailyTarget,
                displayName == null ? "" : displayName
        );
    }

    public static DebugSnapshot debugSnapshot(Context context) {
        SharedPreferences prefs = prefs(context);
        return new DebugSnapshot(
                prefs.getString(KEY_DEBUG_LINE, "No Instagram signals yet."),
                prefs.getLong(KEY_DEBUG_UPDATED_AT, 0L)
        );
    }

    public static Snapshot recordReel(Context context) {
        SharedPreferences prefs = prefs(context);
        rollDayIfNeeded(prefs);
        LocalDate today = LocalDate.now();
        String lastActive = prefs.getString(KEY_LAST_ACTIVE_DAY, "");
        int streak = prefs.getInt(KEY_STREAK, 0);

        if (!today.format(DAY_FORMAT).equals(lastActive)) {
            LocalDate yesterday = today.minusDays(1);
            streak = yesterday.format(DAY_FORMAT).equals(lastActive) ? streak + 1 : 1;
        }

        int todayReels = prefs.getInt(KEY_TODAY_REELS, 0) + 1;
        int totalReels = prefs.getInt(KEY_TOTAL_REELS, 0) + 1;
        prefs.edit()
                .putInt(KEY_TODAY_REELS, todayReels)
                .putInt(KEY_TOTAL_REELS, totalReels)
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_ACTIVE_DAY, today.format(DAY_FORMAT))
                .apply();

        return snapshot(context);
    }

    public static Snapshot recordLike(Context context) {
        SharedPreferences prefs = prefs(context);
        rollDayIfNeeded(prefs);
        prefs.edit()
                .putInt(KEY_TODAY_LIKES, prefs.getInt(KEY_TODAY_LIKES, 0) + 1)
                .putInt(KEY_TOTAL_LIKES, prefs.getInt(KEY_TOTAL_LIKES, 0) + 1)
                .apply();
        return snapshot(context);
    }

    public static Snapshot setDailyTarget(Context context, int target) {
        int safeTarget = Math.max(1, Math.min(target, 20_000));
        prefs(context).edit().putInt(KEY_DAILY_TARGET, safeTarget).apply();
        return snapshot(context);
    }

    public static Snapshot setDisplayName(Context context, String displayName) {
        String safeName = displayName == null ? "" : displayName.trim();
        if (safeName.length() > 24) {
            safeName = safeName.substring(0, 24);
        }
        prefs(context).edit().putString(KEY_DISPLAY_NAME, safeName).apply();
        return snapshot(context);
    }

    public static void recordDebug(Context context, String line) {
        prefs(context).edit()
                .putString(KEY_DEBUG_LINE, line)
                .putLong(KEY_DEBUG_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    /** Get daily history: Map of "2026-06-09" → reelCount. */
    public static java.util.Map<String, Integer> dailyHistory(Context context) {
        java.util.LinkedHashMap<String, Integer> map = new java.util.LinkedHashMap<>();
        String json = prefs(context).getString(KEY_DAILY_HISTORY, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.getInt(key));
            }
        } catch (Exception ignored) {}
        return map;
    }

    /** Get reel count for a specific past date. */
    public static int reelsForDate(Context context, String dateKey) {
        String json = prefs(context).getString(KEY_DAILY_HISTORY, "{}");
        try {
            return new JSONObject(json).optInt(dateKey, -1);
        } catch (Exception e) { return -1; }
    }

    public static String todayKey() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /** Merge cloud total reels — take max of local and cloud. */
    public static void mergeTotalReels(Context context, int cloudTotal) {
        SharedPreferences p = prefs(context);
        int local = p.getInt(KEY_TOTAL_REELS, 0);
        if (cloudTotal > local) {
            p.edit().putInt(KEY_TOTAL_REELS, cloudTotal).apply();
        }
    }

    /** Merge daily history from Supabase JSONArray into local store. */
    public static void mergeDailyHistory(Context context, org.json.JSONArray arr) {
        SharedPreferences p = prefs(context);
        String json = p.getString(KEY_DAILY_HISTORY, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject row = arr.getJSONObject(i);
                String date = row.optString("date", "");
                int reels = row.optInt("reels", 0);
                if (!date.isEmpty() && !obj.has(date)) {
                    obj.put(date, reels);
                }
            }
            while (obj.length() > MAX_HISTORY_DAYS) obj.remove(obj.keys().next());
            p.edit().putString(KEY_DAILY_HISTORY, obj.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void rollDayIfNeeded(SharedPreferences prefs) {
        String today = LocalDate.now().format(DAY_FORMAT);
        String storedToday = prefs.getString(KEY_TODAY, "");
        if (today.equals(storedToday)) {
            return;
        }

        // Save yesterday's count before resetting
        if (!storedToday.isEmpty()) {
            commitToHistory(prefs, storedToday, prefs.getInt(KEY_TODAY_REELS, 0));
        }

        prefs.edit()
                .putString(KEY_TODAY, today)
                .putInt(KEY_TODAY_REELS, 0)
                .putInt(KEY_TODAY_LIKES, 0)
                .apply();
    }

    private static void commitToHistory(SharedPreferences prefs, String dateKey, int reels) {
        String json = prefs.getString(KEY_DAILY_HISTORY, "{}");
        try {
            JSONObject obj = new JSONObject(json);
            if (!obj.has(dateKey)) {
                obj.put(dateKey, reels);
                while (obj.length() > MAX_HISTORY_DAYS) {
                    obj.remove(obj.keys().next());
                }
                prefs.edit().putString(KEY_DAILY_HISTORY, obj.toString()).apply();
            }
        } catch (Exception ignored) {}
    }

    public static final class Snapshot {
        public final int todayReels;
        public final int todayLikes;
        public final int totalReels;
        public final int totalLikes;
        public final int streak;
        public final int xp;
        public final int dailyTarget;
        public final int targetProgressPercent;
        public final String displayName;

        public Snapshot(
                int todayReels,
                int todayLikes,
                int totalReels,
                int totalLikes,
                int streak,
                int xp,
                int dailyTarget,
                String displayName
        ) {
            this.todayReels = todayReels;
            this.todayLikes = todayLikes;
            this.totalReels = totalReels;
            this.totalLikes = totalLikes;
            this.streak = streak;
            this.xp = xp;
            this.dailyTarget = dailyTarget;
            this.targetProgressPercent = dailyTarget <= 0 ? 0 : Math.min(100, Math.round(todayReels * 100f / dailyTarget));
            this.displayName = displayName;
        }
    }

    public static final class DebugSnapshot {
        public final String line;
        public final long updatedAtMs;

        public DebugSnapshot(String line, long updatedAtMs) {
            this.line = line;
            this.updatedAtMs = updatedAtMs;
        }
    }
}
