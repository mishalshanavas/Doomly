package com.doomly.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.doomly.app.data.AuthRepository;
import com.doomly.app.data.StatsRepository;

/** Debounces and coordinates local-stat synchronization with Supabase. */
public final class CloudSyncScheduler {
    private static final String TAG = "DoomlyCloudSync";
    private static final long SYNC_DEBOUNCE_MS = 30_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable pendingSync;

    private CloudSyncScheduler() {}

    public static synchronized void scheduleSync(Context context) {
        Context appContext = context.getApplicationContext();
        if (pendingSync != null) MAIN.removeCallbacks(pendingSync);
        pendingSync = () -> syncNow(appContext, silentCallback());
        MAIN.postDelayed(pendingSync, SYNC_DEBOUNCE_MS);
    }

    public static void syncNow(Context context, ResultCallback<Void> callback) {
        AuthRepository auth = AuthRepository.getInstance();
        if (!auth.isSignedIn(context) || auth.currentUid(context).isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        StatsRepository.getInstance().syncToCloud(context, auth.currentUid(context),
                auth.currentDisplayName(context), "", callback);
    }

    public static void updateDisplayName(Context context, String displayName, ResultCallback<Void> callback) {
        DoomStatsStore.setDisplayName(context, displayName);
        syncNow(context, callback);
    }

    public static void updateDailyTarget(Context context, int target, ResultCallback<Void> callback) {
        DoomStatsStore.setDailyTarget(context, target);
        syncNow(context, callback);
    }

    private static ResultCallback<Void> silentCallback() {
        return new ResultCallback<Void>() {
            @Override public void onSuccess(Void value) {}
            @Override public void onError(String message) { Log.w(TAG, "Sync failed: " + message); }
        };
    }
}
