package com.doomly.app;

/** Cosmetic daily progression; derived only from recorded counts. */
public final class ScrollLevel {
    private static final int[] THRESHOLDS = {0, 25, 75, 150, 300};
    private static final String[] NAMES = {"Fresh thumb", "Casual cruiser", "Feed regular", "Scroll royalty", "Certified legend"};
    public final int index;
    public final String title;
    public final int next;
    public final float progress;
    private ScrollLevel(int index, int count) {
        this.index = index;
        title = NAMES[index];
        next = index == THRESHOLDS.length - 1 ? 0 : THRESHOLDS[index + 1];
        progress = next == 0 ? 1f : (float) (count - THRESHOLDS[index]) / (next - THRESHOLDS[index]);
    }
    public static ScrollLevel forCount(int reels) {
        int count = Math.max(0, reels), level = 0;
        while (level + 1 < THRESHOLDS.length && count >= THRESHOLDS[level + 1]) level++;
        return new ScrollLevel(level, count);
    }
}
