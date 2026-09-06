package com.doomly.app.data;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import com.doomly.app.DoomStatsStore;
import com.doomly.app.ResultCallback;

import org.json.JSONObject;

/**
 * Supabase Google OAuth via Chrome Custom Tabs.
 * After sign-in, Supabase redirects to com.doomly.app://auth/callback
 * which triggers MainActivity.onNewIntent.
 */
public final class AuthRepository {
    private static final String TAG = "DoomlyAuth";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "display_name";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_REFRESH = "refresh_token";

    private static AuthRepository instance;
    public ResultCallback<Void> pendingCallback;

    private AuthRepository() {}

    public static synchronized AuthRepository getInstance() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }

    public boolean isConfigured(Context context) {
        SupabaseConfig cfg = SupabaseClient.getInstance() != null
                ? SupabaseClient.getInstance().getConfig() : null;
        return cfg != null && cfg.isConfigured();
    }

    public boolean isSignedIn(Context context) {
        if (!isConfigured(context)) return false;
        String token = SecureAuthStore.get(context, KEY_TOKEN);
        if (token.isEmpty()) return false;

        // Check local JWT expiry — no network call
        if (isTokenExpired(token)) {
            // Token is expired locally — will try async refresh elsewhere
            // For now, still set session so API calls can try (server may still accept briefly)
        }
        SupabaseClient.getInstance().setSession(token);
        return true;
    }

    /** Check if JWT is expired by decoding the exp claim locally. No network. */
    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return false;
            // Decode payload (base64url)
            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
            long exp = new org.json.JSONObject(payload).optLong("exp", 0);
            return exp > 0 && (exp * 1000L) < System.currentTimeMillis();
        } catch (Exception e) { return false; }
    }

    /** Async refresh token. Call from background thread. */
    public void refreshTokenIfNeeded(Context context) {
        String token = SecureAuthStore.get(context, KEY_TOKEN);
        String refresh = SecureAuthStore.get(context, KEY_REFRESH);
        if (token.isEmpty() || refresh.isEmpty()) return;
        if (!isTokenExpired(token)) return; // Still valid

        // Token expired — try refresh on background thread
        new Thread(() -> {
            if (SupabaseClient.getInstance().refreshSession(refresh)) {
                String newToken = SupabaseClient.getInstance().getAccessToken();
                if (newToken != null && !newToken.isEmpty()) {
                    SecureAuthStore.put(context, KEY_TOKEN, newToken);
                }
            }
        }).start();
    }

    public String currentUid(Context context) {
        return SecureAuthStore.get(context, KEY_UID);
    }
    public String currentEmail(Context context) {
        return SecureAuthStore.get(context, KEY_EMAIL);
    }
    public String currentDisplayName(Context context) {
        return SecureAuthStore.get(context, KEY_NAME);
    }

    /** Start Google OAuth in Chrome Custom Tab. */
    public void signIn(Activity activity, ResultCallback<Void> callback) {
        if (!isConfigured(activity)) {
            callback.onError("Supabase not configured.");
            return;
        }
        pendingCallback = callback;
        SupabaseConfig config = SupabaseClient.getInstance().getConfig();
        String url = config.googleOAuthUrl();
        Log.d(TAG, "Opening OAuth: " + url);

        try {
            new CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(url));
        } catch (Exception e) {
            Log.e(TAG, "CustomTabs failed", e);
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    /** Handle deep link from Chrome after Google OAuth completes. */
    public boolean handleDeepLink(Activity activity, Uri uri) {
        if (uri == null) return false;
        // Never log the full callback URI: its fragment contains session tokens.
        Log.d(TAG, "OAuth callback received for " + uri.getScheme() + "://" + uri.getAuthority() + uri.getPath());

        // Tokens come as fragment: #access_token=xxx&refresh_token=yyy&...
        String fragment = uri.getFragment();
        if (fragment == null || fragment.isEmpty()) {
            // Maybe query params
            fragment = uri.getQuery();
        }
        if (fragment == null || fragment.isEmpty()) {
            fireError("No token in callback");
            return true;
        }

        String accessToken = null, refreshToken = null;
        for (String param : fragment.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "access_token".equals(kv[0])) accessToken = kv[1];
            if (kv.length == 2 && "refresh_token".equals(kv[0])) refreshToken = kv[1];
        }

        if (accessToken == null) {
            fireError("No access token");
            return true;
        }

        SupabaseClient.getInstance().setSession(accessToken);
        fetchUser(activity, accessToken, refreshToken);
        return true;
    }

    private void fetchUser(Activity activity, String token, String refresh) {
        SupabaseConfig config = SupabaseClient.getInstance().getConfig();
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(config.authBaseUrl() + "/user")
                .header("apikey", config.anonKey)
                .header("Authorization", "Bearer " + token)
                .get().build();

        SupabaseClient.getInstance().getHttp().newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call c, java.io.IOException e) {
                Log.e(TAG, "Fetch user failed", e);
                activity.runOnUiThread(() -> fireError(e.getMessage()));
            }

            @Override
            public void onResponse(okhttp3.Call c, okhttp3.Response r) throws java.io.IOException {
                try (okhttp3.ResponseBody body = r.body()) {
                    String text = body != null ? body.string() : "";
                    if (!r.isSuccessful()) {
                        activity.runOnUiThread(() -> fireError("HTTP " + r.code()));
                        return;
                    }
                    org.json.JSONObject json = new org.json.JSONObject(text);
                    String uid = json.getString("id");
                    String email = json.optString("email", "");
                    JSONObject meta = json.optJSONObject("user_metadata");
                    String name = meta != null ? meta.optString("full_name", "") : "";

                    SecureAuthStore.put(activity, KEY_UID, uid);
                    SecureAuthStore.put(activity, KEY_EMAIL, email);
                    SecureAuthStore.put(activity, KEY_NAME, name);
                    SecureAuthStore.put(activity, KEY_TOKEN, token);
                    SecureAuthStore.put(activity, KEY_REFRESH, refresh != null ? refresh : "");

                    if (!name.isEmpty()) DoomStatsStore.setDisplayName(activity, name);
                    Log.d(TAG, "Google sign-in success");

                    // Pull existing cloud data to restore user's stats
                    StatsRepository.getInstance().pullFromCloud(activity, uid, new ResultCallback<Void>() {
                        @Override public void onSuccess(Void v) { Log.d(TAG, "Cloud pull complete"); }
                        @Override public void onError(String m) { Log.w(TAG, "Cloud pull failed: " + m); }
                    });

                    activity.runOnUiThread(() -> fireError(null)); // null = success
                } catch (Exception e) {
                    Log.e(TAG, "Parse", e);
                    activity.runOnUiThread(() -> fireError(e.getMessage()));
                }
            }
        });
    }

    private void fireError(String error) {
        ResultCallback<Void> cb = pendingCallback;
        pendingCallback = null;
        if (cb == null) return;
        if (error != null) cb.onError(error); else cb.onSuccess(null);
    }

    public void signOut(Context context) {
        SupabaseClient client = SupabaseClient.getInstance();
        if (client != null) client.clearSession();
        SecureAuthStore.clear(context);
    }

    public static String safeDisplayName(String name) {
        String safe = (name == null || name.trim().isEmpty()) ? "Doomly user" : name.trim();
        return safe.length() > 24 ? safe.substring(0, 24) : safe;
    }
}
