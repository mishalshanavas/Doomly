package com.doomly.app;

import android.app.NotificationManager;
import android.app.Application;
import android.content.pm.ApplicationInfo;

import java.io.File;

import com.doomly.app.data.SupabaseClient;
import com.doomly.app.data.SupabaseConfig;

public class DoomlyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SupabaseConfig config = SupabaseConfig.from(this);
        if (config.isConfigured()) SupabaseClient.init(config);

        DoomStatsStore.snapshot(this);
        DoomlyNotifications.createChannel(this);
        removeLegacyDiagnostics();
    }

    /** Removes artifacts created by pre-production builds without touching user stats or auth. */
    private void removeLegacyDiagnostics() {
        getSharedPreferences("doomly_stats", MODE_PRIVATE).edit()
                .remove("debug_line")
                .remove("debug_updated_at")
                .apply();

        NotificationManager notifications = getSystemService(NotificationManager.class);
        if (notifications != null) notifications.deleteNotificationChannel("doomly_foreground");

        boolean debugBuild = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debugBuild) return;
        File external = getExternalFilesDir(null);
        if (external == null) return;
        File logDirectory = new File(external, "logs");
        new File(logDirectory, "reel-detector.log").delete();
        new File(logDirectory, "reel-detector.previous.log").delete();
        logDirectory.delete();
    }
}
