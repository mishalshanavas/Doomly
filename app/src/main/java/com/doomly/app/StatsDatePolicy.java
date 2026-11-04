package com.doomly.app;

import java.time.LocalDate;

/** Display a streak through yesterday, but never across an inactive day. */
public final class StatsDatePolicy {
    private StatsDatePolicy() {}
    public static int visibleStreak(int stored, String lastActive, LocalDate today) {
        try {
            LocalDate last = LocalDate.parse(lastActive);
            return last.equals(today) || last.equals(today.minusDays(1)) ? Math.max(0, stored) : 0;
        } catch (Exception ignored) { return 0; }
    }
}
