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
}
