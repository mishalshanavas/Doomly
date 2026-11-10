package com.doomly.app;

/** Pure, deterministic motivation rules shared by the UI and notifications. */
public final class MotivationEngine {
    private MotivationEngine() {}

    public static int nextMilestone(int reels, int target) {
        int safeReels = Math.max(0, reels);
        int safeTarget = Math.max(1, target);
        int next = (safeReels / 25 + 1) * 25;
        return safeReels < safeTarget ? Math.min(safeTarget, next) : next;
    }

    public static int reachedMilestone(int reels, int target) {
        int count = Math.max(0, reels);
        return Math.max(count / 25 * 25, count >= Math.max(1, target) ? Math.max(1, target) : 0);
    }

}
