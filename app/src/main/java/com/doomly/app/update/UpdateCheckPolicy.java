package com.doomly.app.update;

public final class UpdateCheckPolicy {
    private static final long AUTOMATIC_INTERVAL_MS = 15L * 60_000L;
    private UpdateCheckPolicy() {}

    public static boolean shouldCheck(long now, long lastCheck) {
        return lastCheck < 0 || now < lastCheck || now - lastCheck >= AUTOMATIC_INTERVAL_MS;
    }
}
