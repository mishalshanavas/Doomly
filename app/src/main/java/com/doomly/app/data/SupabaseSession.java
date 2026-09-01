package com.doomly.app.data;

/**
 * Represents a Supabase authentication session.
 */
public final class SupabaseSession {
    public final String uid;
    public final String email;
    public final String displayName;
    public final String accessToken;
    public final String refreshToken;

    public SupabaseSession(String uid, String email, String displayName,
                           String accessToken, String refreshToken) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName != null ? displayName : "";
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
