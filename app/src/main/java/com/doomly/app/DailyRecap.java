package com.doomly.app;

import java.time.LocalDate;
import java.util.Map;

public final class DailyRecap {
    public final LocalDate date;
    public final int reels;
    public final int goal;
    public final int streak;
    public final int[] weekCounts;

    public DailyRecap(LocalDate date, int reels, int goal, int streak, Map<String, Integer> history) {
        this.date = date;
        this.reels = Math.max(0, reels);
        this.goal = Math.max(1, goal);
        this.streak = Math.max(0, streak);
        weekCounts = new int[7];
        for (int i = 0; i < 6; i++) {
            weekCounts[i] = Math.max(0, history.getOrDefault(date.minusDays(6 - i).toString(), 0));
        }
        weekCounts[6] = this.reels;
    }

    public int goalPercent() {
        return (int) Math.min(100, Math.round(reels * 100.0 / goal));
    }

    public String headline() {
        return reels == 0 ? "A quiet scroll day." : reels < goal ? "Your day, in Reels." : "That's a wrap.";
    }
}
