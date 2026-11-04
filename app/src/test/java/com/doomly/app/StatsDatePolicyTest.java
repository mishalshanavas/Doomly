package com.doomly.app;

import org.junit.Test;
import java.time.LocalDate;
import static org.junit.Assert.assertEquals;

public class StatsDatePolicyTest {
    private final LocalDate today = LocalDate.of(2026, 11, 4);
    @Test public void keepsStreakThroughYesterday() {
        assertEquals(5, StatsDatePolicy.visibleStreak(5, "2026-11-03", today));
        assertEquals(5, StatsDatePolicy.visibleStreak(5, "2026-11-04", today));
    }
    @Test public void missedDayResetsDisplayBeforeAnotherReel() {
        assertEquals(0, StatsDatePolicy.visibleStreak(5, "2026-11-02", today));
    }
    @Test public void invalidAndFutureDatesDoNotClaimAStreak() {
        assertEquals(0, StatsDatePolicy.visibleStreak(5, "", today));
        assertEquals(0, StatsDatePolicy.visibleStreak(5, "2026-11-05", today));
    }
}
