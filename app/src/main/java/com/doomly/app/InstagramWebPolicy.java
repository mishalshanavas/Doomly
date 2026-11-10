package com.doomly.app;

import java.util.Locale;

/** Pure policy for the in-app Instagram browser. Unit-tested; no Android deps. */
public final class InstagramWebPolicy {
    private InstagramWebPolicy() {}

    /** Authenticated iff Instagram set a session cookie. `sessionid` / `ds_user_id` are only issued post-login. */
    public static boolean isAuthenticatedCookie(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) return false;
        for (String part : cookieHeader.split(";")) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String name = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = p.substring(eq + 1).trim();
            if ((name.equals("sessionid") || name.equals("ds_user_id")) && !value.isEmpty()
                    && !value.equals("null") && !value.equals("\"\"")) return true;
        }
        return false;
    }

    public static boolean isInstagramHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("instagram.com") || h.endsWith(".instagram.com")
                || h.equals("facebook.com") || h.endsWith(".facebook.com")
                || h.equals("fbcdn.net") || h.endsWith(".fbcdn.net");
    }

    public static boolean isLoginUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("/accounts/login") || u.contains("/accounts/signup") || u.contains("checkpoint");
    }

    public static boolean isReelsUrl(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).contains("instagram.com/reel");
    }

    /** HTTP statuses that must surface our error state instead of Chromium's error page. */
    public static boolean isErrorStatus(int statusCode) { return statusCode == -1 || statusCode >= 400; }

    /** User-facing message for a failed main-frame load, or null when not an error. */
    public static String errorMessage(int statusCode) {
        if (!isErrorStatus(statusCode)) return null;
        if (statusCode == 429) return "Too many requests. Try again later.";
        if (statusCode == -1) return "Couldn't load. Check connection.";
        return "Instagram error " + statusCode;
    }

    /** Schemes the WebView must not try to load itself. */
    public static boolean isExternalScheme(String scheme) {
        if (scheme == null) return true;
        String s = scheme.toLowerCase(Locale.ROOT);
        return !(s.equals("https") || s.equals("http") || s.equals("about") || s.equals("data") || s.equals("blob"));
    }
}
