package com.doomly.app;

import java.time.LocalDate;

/** One small, deterministic achievement per local calendar day. */
public final class DailyAchievement {
    private static final String[] TITLES = {
        "Briefly unavailable", "The scenic route", "Field research",
        "Calendar conflict", "Out of office", "A small diversion", "Extended lunch"
    };
    private static final int[] TARGETS = {3, 5, 7, 4, 10, 5, 8};
    public final String title;
    public final int target;

    private DailyAchievement(String title, int target) { this.title = title; this.target = target; }

    public static DailyAchievement forDay(LocalDate day, int dailyGoal) {
        int index = (int) Math.floorMod(day.toEpochDay(), (long) TITLES.length);
        return new DailyAchievement(TITLES[index], Math.min(TARGETS[index], Math.max(1, dailyGoal)));
    }

    public boolean earned(int reels) { return reels >= target; }
    public int progress(int reels) { return Math.min(target, Math.max(0, reels)); }
}
