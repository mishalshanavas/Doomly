package com.doomly.app;

/** Pure, deterministic motivation rules shared by the UI and notifications. */
public final class MotivationEngine {
    private MotivationEngine() {}

    public static int remaining(int reels, int target) {
        return Math.max(0, Math.max(1, target) - Math.max(0, reels));
    }

    public static int level(int totalReels) {
        return Math.max(1, Math.max(0, totalReels) / 250 + 1);
    }

    public static int nextMilestone(int reels, int target) {
        int safeReels = Math.max(0, reels);
        int safeTarget = Math.max(1, target);
        int step = Math.max(25, Math.min(250, safeTarget / 10));
        return Math.min(safeTarget, ((safeReels / step) + 1) * step);
    }

    public static String message(int reels, int target, int streak) {
        int safeTarget = Math.max(1, target);
        int remaining = remaining(reels, safeTarget);
        if (reels <= 0) return "Your first Reel starts today's streak. Let's roll.";
        if (remaining == 0) return "Goal crushed! Keep scrolling to set a new personal best.";
        float progress = reels / (float) safeTarget;
        if (progress >= 0.9f) return "Final stretch: only " + remaining + " Reels to your goal.";
        if (progress >= 0.5f) return "You're over halfway there. Keep the streak moving!";
        if (streak > 1) return streak + "-day streak in motion. Every Reel adds up.";
        return "Nice start! Next milestone: " + nextMilestone(reels, safeTarget) + " Reels.";
    }
}
