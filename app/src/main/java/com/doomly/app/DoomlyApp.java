package com.doomly.app;

import android.app.Application;

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
        DoomlyUpdater.check(this);
    }
}
