package com.doomly.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class NotificationPolicyTest {
    @Test public void firstCelebrationAllowed() { assertTrue(NotificationPolicy.allow(1000, 0, 0, false)); }
    @Test public void rapidMilestonesDoNotSpam() {
        assertFalse(NotificationPolicy.allow(2000, 1000, 1, false));
        assertTrue(NotificationPolicy.allow(1000 + NotificationPolicy.COOLDOWN_MS, 1000, 1, false));
    }
    @Test public void dailyRegularCelebrationCap() { assertFalse(NotificationPolicy.allow(9_000_000, 1000, 4, false)); }
    @Test public void firstGoalCanCelebrateDespiteCooldownOrCap() { assertTrue(NotificationPolicy.allow(2000, 1000, 4, true)); }
    @Test public void clockMovingBackwardDoesNotBypassCooldown() { assertFalse(NotificationPolicy.allow(500, 1000, 1, false)); }
}
