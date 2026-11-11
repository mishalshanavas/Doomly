package com.doomly.app;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.Assert.*;

public class DailyRecapTest {
    private final LocalDate today = LocalDate.of(2026, 11, 11);

    @Test public void defaultDailyGoalIsTwoHundred() {
        assertEquals(200, DoomStatsStore.DEFAULT_DAILY_TARGET);
    }

    @Test public void goalReachedHeadlineUsesCurrentGoal() {
        assertEquals("That's a wrap.", new DailyRecap(today, 200, 200, 0, Map.of()).headline());
        assertEquals("Your day, in Reels.", new DailyRecap(today, 199, 200, 0, Map.of()).headline());
    }

    @Test public void weekEndsWithCurrentCountNotStaleHistory() {
        DailyRecap recap = new DailyRecap(today, 120, 200, 3, Map.of(
                today.toString(), 99, today.minusDays(1).toString(), 80,
                today.minusDays(6).toString(), 40, today.minusDays(7).toString(), 500));
        assertArrayEquals(new int[]{40, 0, 0, 0, 0, 80, 120}, recap.weekCounts);
        assertEquals(60, recap.goalPercent());
    }

    @Test public void progressCapsAtOneHundred() {
        assertEquals(100, new DailyRecap(today, 400, 200, 0, Map.of()).goalPercent());
        assertEquals(100, new DailyRecap(today, Integer.MAX_VALUE, 1, 0, Map.of()).goalPercent());
    }

    @Test public void emptyDayHasQuietHeadline() {
        DailyRecap recap = new DailyRecap(today, 0, 200, 0, Map.of());
        assertEquals(0, recap.goalPercent());
        assertEquals("A quiet scroll day.", recap.headline());
    }

    @Test public void invalidInputsStaySafe() {
        DailyRecap recap = new DailyRecap(today, -1, 0, -2, Map.of(today.minusDays(1).toString(), -20));
        assertEquals(1, recap.goal);
        assertEquals(0, recap.reels);
        assertEquals(0, recap.streak);
        assertArrayEquals(new int[7], recap.weekCounts);
    }
}
