package com.doomly.app.data;

import android.content.Context;

import com.doomly.app.DoomStatsStore;
import com.doomly.app.ResultCallback;

/**
 * Single source of truth for doom-scroll stats.
 * Writes locally first (instant), then syncs to Supabase.
 */
public final class StatsRepository {

    private static StatsRepository instance;

    private StatsRepository() {}

    public static synchronized StatsRepository getInstance() {
        if (instance == null) instance = new StatsRepository();
        return instance;
    }

    public DoomStatsStore.Snapshot getSnapshot(Context context) {
        return DoomStatsStore.snapshot(context);
    }

    public DoomStatsStore.Snapshot recordReel(Context context) {
        return DoomStatsStore.recordReel(context);
    }

    public DoomStatsStore.Snapshot setDailyTarget(Context context, int target) {
        return DoomStatsStore.setDailyTarget(context, target);
    }

    public DoomStatsStore.Snapshot setDisplayName(Context context, String name) {
        return DoomStatsStore.setDisplayName(context, name);
    }

    /** Push current local stats to Supabase. */
    public void syncToCloud(Context context, String uid, String displayName,
                            String photoUrl, ResultCallback<Void> callback) {
        DoomStatsStore.Snapshot snap = DoomStatsStore.snapshot(context);
        SupabaseClient client = SupabaseClient.getInstance();
        if (client == null || !client.getConfig().isConfigured()) { callback.onSuccess(null); return; }

        client.upsertUser(uid, displayName, snap.dailyTarget, snap.totalReels, snap.streak, snap.streak,
            new ResultCallback<Void>() {
                @Override public void onSuccess(Void v) {
                    client.upsertDaily(uid, displayName, snap.todayReels, callback);
                }
                @Override public void onError(String m) { callback.onError(m); }
            });
    }

    /** Pull existing user data from Supabase and merge into local store. */
    public void pullFromCloud(Context context, String uid, ResultCallback<Void> callback) {
        SupabaseClient client = SupabaseClient.getInstance();
        if (client == null || !client.getConfig().isConfigured()) { callback.onSuccess(null); return; }

        client.fetchUser(uid, new ResultCallback<org.json.JSONObject>() {
            @Override public void onSuccess(org.json.JSONObject user) {
                try {
                    if (user.has("daily_target")) { int t = user.getInt("daily_target"); if (t > 0) DoomStatsStore.setDailyTarget(context, t); }
                    if (user.has("total_reels")) { int tr = user.getInt("total_reels"); DoomStatsStore.Snapshot local = DoomStatsStore.snapshot(context); if (tr > local.totalReels) DoomStatsStore.mergeTotalReels(context, tr); }
                    if (user.has("display_name")) { String n = user.optString("display_name", ""); if (!n.isEmpty()) DoomStatsStore.setDisplayName(context, n); }
                } catch (Exception e) { callback.onError("Invalid profile data: " + e.getMessage()); return; }
                client.fetchHistory(uid, new ResultCallback<org.json.JSONArray>() {
                    @Override public void onSuccess(org.json.JSONArray arr) { DoomStatsStore.mergeDailyHistory(context, arr); callback.onSuccess(null); }
                    @Override public void onError(String m) { callback.onError(m); }
                });
            }
            @Override public void onError(String m) { callback.onError(m); }
        });
    }
}
