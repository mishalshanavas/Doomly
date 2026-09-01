package com.doomly.app.doomstats;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight recent activity log stored in SharedPreferences.
 * Records when reels are counted so the Today screen shows a feed.
 */
public final class RecentActivityLog {

    private static final String PREFS = "doomly_activity_log";
    private static final String KEY_LOG = "activity_entries";
    private static final int MAX_ENTRIES = 50;
    private static final long SESSION_MERGE_MS = 120_000L; // 2 min — merge within same session

    private RecentActivityLog() {}

    /**
     * Record a reel-count event. Merges into the most recent entry if within 2 minutes
     * (same session), otherwise creates a new session entry.
     */
    public static void record(Context context, int reels) {
        List<Entry> entries = getAll(context);
        long now = System.currentTimeMillis();

        // Merge into most recent entry if within the same session window
        if (!entries.isEmpty() && (now - entries.get(0).timestamp) < SESSION_MERGE_MS) {
            Entry merged = new Entry(entries.get(0).reels + reels, entries.get(0).timestamp);
            entries.set(0, merged);
        } else {
            entries.add(0, new Entry(reels, now));
            while (entries.size() > MAX_ENTRIES) {
                entries.remove(entries.size() - 1);
            }
        }
        saveAll(context, entries);
    }

    /**
     * Get the N most recent entries.
     */
    public static List<Entry> getRecent(Context context, int count) {
        List<Entry> all = getAll(context);
        return all.subList(0, Math.min(count, all.size()));
    }

    private static List<Entry> getAll(Context context) {
        List<Entry> entries = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LOG, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                entries.add(new Entry(
                        obj.getInt("reels"),
                        obj.getLong("ts")
                ));
            }
        } catch (JSONException ignored) {}
        return entries;
    }

    private static void saveAll(Context context, List<Entry> entries) {
        JSONArray arr = new JSONArray();
        for (Entry e : entries) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("reels", e.reels);
                obj.put("ts", e.timestamp);
                arr.put(obj);
            } catch (JSONException ignored) {}
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOG, arr.toString())
                .apply();
    }

    /**
     * Clear all log entries.
     */
    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LOG)
                .apply();
    }

    public static final class Entry {
        public final int reels;
        public final long timestamp;

        public Entry(int reels, long timestamp) {
            this.reels = reels;
            this.timestamp = timestamp;
        }
    }
}
