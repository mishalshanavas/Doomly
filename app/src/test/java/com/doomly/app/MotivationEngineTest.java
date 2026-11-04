package com.doomly.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MotivationEngineTest {
    @Test public void remainingNeverGoesNegative() {
        assertEquals(0, MotivationEngine.remaining(120, 100));
    }

    @Test public void levelAdvancesEvery250Reels() {
        assertEquals(1, MotivationEngine.level(249));
        assertEquals(2, MotivationEngine.level(250));
    }

    @Test public void messageEncouragesGoalCompletion() {
        assertTrue(MotivationEngine.message(95, 100, 2).contains("5 Reels"));
        assertTrue(MotivationEngine.message(100, 100, 2).contains("Goal crushed"));
    }
    @Test public void uiAndNotificationsUseSameMilestonesIncludingSmallGoals() {
        assertEquals(0, MotivationEngine.reachedMilestone(24, 100));
        assertEquals(25, MotivationEngine.reachedMilestone(25, 100));
        assertEquals(10, MotivationEngine.reachedMilestone(10, 10));
        assertEquals(80, MotivationEngine.reachedMilestone(80, 80));
        assertEquals(100, MotivationEngine.reachedMilestone(101, 80));
    }
    @Test public void nextMilestoneRemainsAheadAfterGoal() {
        assertEquals(25, MotivationEngine.nextMilestone(0, 100));
        assertEquals(80, MotivationEngine.nextMilestone(79, 80));
        assertEquals(100, MotivationEngine.nextMilestone(80, 80));
        assertEquals(125, MotivationEngine.nextMilestone(100, 100));
    }
}
