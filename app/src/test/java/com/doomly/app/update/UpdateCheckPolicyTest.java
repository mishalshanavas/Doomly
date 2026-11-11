package com.doomly.app.update;

import org.junit.Test;
import static org.junit.Assert.*;

public class UpdateCheckPolicyTest {
    @Test public void firstForegroundChecksImmediately() {
        assertTrue(UpdateCheckPolicy.shouldCheck(0, -1));
    }

    @Test public void repeatedForegroundDoesNotExhaustGithubRateLimit() {
        assertFalse(UpdateCheckPolicy.shouldCheck(1000, 100));
        assertFalse(UpdateCheckPolicy.shouldCheck(900099, 100));
        assertTrue(UpdateCheckPolicy.shouldCheck(900100, 100));
    }

    @Test public void clockResetAllowsChecking() {
        assertTrue(UpdateCheckPolicy.shouldCheck(100, 200));
    }
}
