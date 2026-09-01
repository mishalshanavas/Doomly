package com.doomly.app.data;

import android.content.Context;

import com.doomly.app.ResultCallback;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads the daily leaderboard from Supabase.
 */
public final class LeaderboardRepository {

    private static LeaderboardRepository instance;

    private LeaderboardRepository() {}

    public static synchronized LeaderboardRepository getInstance() {
        if (instance == null) instance = new LeaderboardRepository();
        return instance;
    }

    public void loadLeaderboard(Context context, ResultCallback<List<LeaderboardEntry>> callback) {
        SupabaseClient client = SupabaseClient.getInstance();
        if (client == null || !client.getConfig().isConfigured()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        client.fetchLeaderboard(new ResultCallback<org.json.JSONArray>() {
            @Override
            public void onSuccess(org.json.JSONArray arr) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                int rank = 1;
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject obj = arr.getJSONObject(i);
                        entries.add(new LeaderboardEntry(
                                rank++,
                                obj.optString("uid", ""),
                                obj.optString("display_name", "Doomly user"),
                                obj.optInt("reels", 0)));
                    } catch (Exception ignored) {}
                }
                callback.onSuccess(entries);
            }

            @Override
            public void onError(String message) { callback.onError(message); }
        });
    }

    public static final class LeaderboardEntry {
        public final int rank;
        public final String uid;
        public final String displayName;
        public final int reels;

        public LeaderboardEntry(int rank, String uid, String displayName, int reels) {
            this.rank = rank; this.uid = uid != null ? uid : ""; this.displayName = displayName; this.reels = reels;
        }

        public String initials() {
            String t = displayName.trim();
            if (t.isEmpty()) return "D";
            String[] parts = t.split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
    }
}
