package com.doomly.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks GitHub Releases for newer APK versions and prompts download.
 * Call DoomlyUpdater.check(ctx) from MainActivity.onCreate().
 */
public final class DoomlyUpdater {
    private static final String TAG = "DoomlyUpdater";
    private static final String REPO_API = "https://api.github.com/repos/mishal/Doomly/releases/latest";

    private DoomlyUpdater() {}

    /** Check for updates on a background thread. */
    public static void check(Context ctx) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(REPO_API).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
                if (conn.getResponseCode() != 200) return;

                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = r.readLine()) != null) sb.append(line);
                r.close(); conn.disconnect();

                JSONObject release = new JSONObject(sb.toString());
                String tag = release.getString("tag_name").replace("v", "");
                String current = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
                if (compareVersions(tag, current) > 0) {
                    // New version available — find the APK asset
                    var assets = release.getJSONArray("assets");
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String name = asset.getString("name");
                        if (name.endsWith(".apk")) {
                            String url = asset.getString("browser_download_url");
                            downloadAndInstall(ctx, url, name, tag);
                            break;
                        }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Update check failed", e); }
        }).start();
    }

    private static void downloadAndInstall(Context ctx, String url, String name, String version) {
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url))
                .setTitle("Doomly v" + version).setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
        DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        long id = dm.enqueue(req);

        // When download completes, prompt install
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        ContextCompat.registerReceiver(ctx, new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                long doneId = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (doneId == id) {
                    Uri uri = dm.getUriForDownloadedFile(id);
                    if (uri != null) {
                        Intent install = new Intent(Intent.ACTION_VIEW);
                        install.setDataAndType(uri, "application/vnd.android.package-archive");
                        install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        ctx.startActivity(install);
                    }
                }
                ctx.unregisterReceiver(this);
            }
        }, filter, ContextCompat.RECEIVER_EXPORTED);
    }

    private static int compareVersions(String a, String b) {
        String[] pa = a.split("\\."); String[] pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length ? Integer.parseInt(pa[i]) : 0;
            int vb = i < pb.length ? Integer.parseInt(pb[i]) : 0;
            if (va != vb) return va - vb;
        }
        return 0;
    }
}
