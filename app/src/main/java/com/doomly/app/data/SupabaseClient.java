package com.doomly.app.data;

import android.util.Log;
import com.doomly.app.ResultCallback;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public final class SupabaseClient {
    private static final String TAG = "DoomlySupabase";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final SupabaseConfig config;
    private final OkHttpClient http;
    private String accessToken;
    private static SupabaseClient instance;

    private SupabaseClient(SupabaseConfig config) {
        this.config = config;
        this.http = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build();
    }

    public static synchronized SupabaseClient init(SupabaseConfig config) {
        if (instance != null && instance.config.url.equals(config.url)) return instance;
        instance = new SupabaseClient(config); return instance;
    }
    public static SupabaseClient getInstance() { return instance; }
    public SupabaseConfig getConfig() { return config; }
    public OkHttpClient getHttp() { return http; }
    public void setSession(String token) { this.accessToken = token; }
    public void clearSession() { this.accessToken = null; }
    public String getAccessToken() { return accessToken; }

    // ── Auth ───────────────────────────────────────────────────────

    public boolean refreshSession(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) return false;
        try {
            JSONObject body = new JSONObject(); body.put("refresh_token", refreshToken);
            Request req = new Request.Builder().url(config.authBaseUrl() + "/token?grant_type=refresh_token")
                    .header("apikey", config.anonKey).post(RequestBody.create(body.toString(), JSON)).build();
            Response res = http.newCall(req).execute();
            if (res.isSuccessful() && res.body() != null) {
                accessToken = new JSONObject(res.body().string()).getString("access_token");
                return true;
            }
        } catch (Exception e) { Log.e(TAG, "Refresh failed", e); }
        accessToken = null; return false;
    }

    // ── DB: Users ──────────────────────────────────────────────────

    public void upsertUser(String uid, String displayName, int dailyTarget, int totalReels, int streak, int longestStreak, ResultCallback<Void> cb) {
        JSONObject b = new JSONObject();
        try { b.put("uid", uid); b.put("display_name", displayName); b.put("daily_target", dailyTarget); b.put("total_reels", totalReels); b.put("streak", streak); b.put("longest_streak", longestStreak); } catch (Exception e) { cb.onError("build"); return; }
        post("/users", b, new JC() { public void onJson(JSONObject j) { cb.onSuccess(null); } public void onError(String m) { patch("/users?uid=eq." + uid, b, new JC() { public void onJson(JSONObject j) { cb.onSuccess(null); } public void onError(String m2) { cb.onError(m2); } }); } });
    }

    public void fetchUser(String uid, ResultCallback<JSONObject> cb) {
        getObj("/users?uid=eq." + uid + "&limit=1", new JC() { public void onJson(JSONObject j) { cb.onSuccess(j); } public void onError(String m) { cb.onError(m); } });
    }

    // ── DB: Daily History ──────────────────────────────────────────

    public void upsertDaily(String uid, String displayName, int reels, ResultCallback<Void> cb) {
        JSONObject b = new JSONObject();
        try { b.put("uid", uid); b.put("display_name", displayName); b.put("date", java.time.LocalDate.now().toString()); b.put("reels", reels); } catch (Exception e) { cb.onError("build"); return; }
        post("/daily_history", b, new JC() { public void onJson(JSONObject j) { cb.onSuccess(null); } public void onError(String m) { cb.onError(m); } });
    }

    public void fetchLeaderboard(ResultCallback<JSONArray> cb) {
        String today = java.time.LocalDate.now().toString();
        get("/daily_history?select=uid,display_name,reels&date=eq." + today + "&order=reels.desc&limit=50", new JAC() { public void onJson(JSONArray a) { cb.onSuccess(a); } public void onError(String m) { cb.onError(m); } });
    }

    public void fetchHistory(String uid, ResultCallback<JSONArray> cb) {
        get("/daily_history?uid=eq." + uid + "&order=date.desc&limit=30", new JAC() { public void onJson(JSONArray a) { cb.onSuccess(a); } public void onError(String m) { cb.onError(m); } });
    }

    /** Register one FCM token for the signed-in Supabase user. */
    public void registerDeviceToken(String uid, String token, ResultCallback<Void> cb) {
        JSONObject body = new JSONObject();
        try {
            body.put("uid", uid);
            body.put("token", token);
            body.put("platform", "android");
        } catch (Exception e) { cb.onError("build"); return; }
        post("/device_tokens", body, new JC() {
            public void onJson(JSONObject ignored) { cb.onSuccess(null); }
            public void onError(String message) { cb.onError(message); }
        });
    }

    // ── HTTP ───────────────────────────────────────────────────────

    private String authHdr() { return "Bearer " + (accessToken != null ? accessToken : config.anonKey); }

    private void post(String path, JSONObject body, JC cb) {
        Request r = new Request.Builder().url(config.restBaseUrl() + path)
                .header("apikey", config.anonKey).header("Authorization", authHdr()).header("Prefer", "resolution=merge-duplicates")
                .post(RequestBody.create(body.toString(), JSON)).build();
        http.newCall(r).enqueue(new Callback() {
            public void onFailure(Call c, IOException e) { cb.onError(safeMessage(e)); }
            public void onResponse(Call c, Response res) throws IOException {
                try (ResponseBody rb = res.body()) { String t = rb != null ? rb.string() : ""; if (!res.isSuccessful()) { cb.onError("HTTP " + res.code()); return; } cb.onJson(t.isEmpty() ? new JSONObject() : new JSONObject(t)); } catch (Exception e) { cb.onError(safeMessage(e)); }
            }
        });
    }

    private void patch(String path, JSONObject body, JC cb) {
        Request r = new Request.Builder().url(config.restBaseUrl() + path)
                .header("apikey", config.anonKey).header("Authorization", authHdr()).header("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON)).build();
        http.newCall(r).enqueue(new Callback() {
            public void onFailure(Call c, IOException e) { cb.onError(safeMessage(e)); }
            public void onResponse(Call c, Response res) throws IOException {
                try (ResponseBody ignored = res.body()) {
                    if (!res.isSuccessful()) { cb.onError("HTTP " + res.code()); return; }
                    cb.onJson(new JSONObject());
                } catch (Exception e) { cb.onError(safeMessage(e)); }
            }
        });
    }

    private void get(String path, JAC cb) {
        Request r = new Request.Builder().url(config.restBaseUrl() + path).header("apikey", config.anonKey).header("Authorization", authHdr()).get().build();
        http.newCall(r).enqueue(new Callback() {
            public void onFailure(Call c, IOException e) { cb.onError(safeMessage(e)); }
            public void onResponse(Call c, Response res) throws IOException {
                try (ResponseBody rb = res.body()) { String t = rb != null ? rb.string() : ""; if (!res.isSuccessful()) { cb.onError("HTTP " + res.code()); return; } cb.onJson(new JSONArray(t)); } catch (Exception e) { cb.onError(safeMessage(e)); }
            }
        });
    }

    private void getObj(String path, JC cb) {
        Request r = new Request.Builder().url(config.restBaseUrl() + path).header("apikey", config.anonKey).header("Authorization", authHdr()).get().build();
        http.newCall(r).enqueue(new Callback() {
            public void onFailure(Call c, IOException e) { cb.onError(safeMessage(e)); }
            public void onResponse(Call c, Response res) throws IOException {
                try (ResponseBody rb = res.body()) { String t = rb != null ? rb.string() : ""; if (!res.isSuccessful()) { cb.onError("HTTP " + res.code()); return; }
                    JSONArray arr = new JSONArray(t); cb.onJson(arr.length() > 0 ? arr.getJSONObject(0) : new JSONObject()); } catch (Exception e) { cb.onError(safeMessage(e)); }
            }
        });
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }

    private interface JC { void onJson(JSONObject j); void onError(String m); }
    private interface JAC { void onJson(JSONArray a); void onError(String m); }
}
