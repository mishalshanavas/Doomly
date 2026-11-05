package com.doomly.app;

import org.junit.Test;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class DailyAchievementTest {
    private final LocalDate day = LocalDate.of(2026, 11, 5);
    @Test public void selectionIsStableForTheSameDay() {
        DailyAchievement first = DailyAchievement.forDay(day, 100);
        DailyAchievement again = DailyAchievement.forDay(day, 100);
        assertEquals(first.title, again.title);
        assertEquals(first.target, again.target);
    }
    @Test public void aWeekHasSevenDistinctSmallAchievements() {
        Set<String> titles = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            DailyAchievement badge = DailyAchievement.forDay(day.plusDays(i), 100);
            titles.add(badge.title);
            assertTrue(badge.target > 0 && badge.target <= 10);
        }
        assertEquals(7, titles.size());
    }
    @Test public void targetNeverExceedsTheChosenDailyGoal() {
        for (int i = 0; i < 7; i++) assertEquals(1, DailyAchievement.forDay(day.plusDays(i), 1).target);
        assertEquals(1, DailyAchievement.forDay(day, 0).target);
    }
    @Test public void unlockRequiresTheActualThreshold() {
        DailyAchievement badge = DailyAchievement.forDay(day, 100);
        assertFalse(badge.earned(badge.target - 1));
        assertTrue(badge.earned(badge.target));
        assertEquals(0, badge.progress(-1));
        assertEquals(badge.target, badge.progress(1000));
    }
    @Test public void datesBeforeEpochAlsoWork() {
        assertNotNull(DailyAchievement.forDay(LocalDate.of(1960, 1, 1), 100).title);
    }
}
