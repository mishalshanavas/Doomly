package com.doomly.app.data;

import android.content.Context;
import android.net.Uri;

import com.doomly.app.R;

/**
 * Holds Supabase project configuration loaded from strings.xml.
 * Replace YOUR_PROJECT and YOUR_ANON_KEY with real values.
 */
public final class SupabaseConfig {

    public final String url;
    public final String anonKey;

    public SupabaseConfig(String url, String anonKey) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.anonKey = anonKey;
    }

    public static SupabaseConfig from(Context context) {
        return new SupabaseConfig(
                context.getString(R.string.supabase_url),
                context.getString(R.string.supabase_anon_key)
        );
    }

    public boolean isConfigured() {
        return url != null && !url.isEmpty()
                && !url.contains("YOUR_PROJECT")
                && anonKey != null && !anonKey.isEmpty()
                && !anonKey.contains("YOUR_ANON");
    }

    /** OAuth redirect URI for deep link callback. Use package-based scheme. */
    public String redirectUri() {
        return "com.doomly.app://auth/callback";
    }

    /** Build the Supabase OAuth authorize URL for Google provider. */
    public String googleOAuthUrl() {
        return url + "/auth/v1/authorize?provider=google&redirect_to="
                + Uri.encode(redirectUri());
    }

    public String authBaseUrl() { return url + "/auth/v1"; }
    public String restBaseUrl() { return url + "/rest/v1"; }
}
