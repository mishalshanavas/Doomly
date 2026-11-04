package com.doomly.app;

/** Bound background celebrations while allowing the first daily goal celebration. */
public final class NotificationPolicy {
    public static final long COOLDOWN_MS = 5 * 60 * 1000L;
    private NotificationPolicy() {}
    public static boolean allow(long now, long last, int sentToday, boolean newGoal) {
        if (newGoal) return true;
        return sentToday < 4 && (last == 0 || now - last >= COOLDOWN_MS);
    }
}
