package com.doomly.app;

import android.content.Context;
/** @deprecated Use {@link CloudSyncScheduler}; Doomly uses Supabase, not Firebase. */
@Deprecated
public final class FirebaseRepo {
    private FirebaseRepo() {}

    public static void scheduleSync(Context context) {
        CloudSyncScheduler.scheduleSync(context);
    }

    public static void syncNow(Context context, ResultCallback<Void> callback) {
        CloudSyncScheduler.syncNow(context, callback);
    }

    public static void updateDisplayName(Context context, String displayName, ResultCallback<Void> callback) {
        CloudSyncScheduler.updateDisplayName(context, displayName, callback);
    }

    public static void updateDailyTarget(Context context, int target, ResultCallback<Void> callback) {
        CloudSyncScheduler.updateDailyTarget(context, target, callback);
    }
}
