package com.doomly.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class RecentFingerprintCacheTest {
    @Test public void revisitingOlderReelIsDeduplicated() {
        RecentFingerprintCache cache = new RecentFingerprintCache(300_000L, 150);
        cache.remember("reel-a", 1_000L);
        cache.remember("reel-b", 2_000L);

        assertTrue(cache.contains("reel-a", 3_000L));
    }

    @Test public void expiredFingerprintCanBeCountedAgain() {
        RecentFingerprintCache cache = new RecentFingerprintCache(300_000L, 150);
        cache.remember("reel-a", 1_000L);

        assertFalse(cache.contains("reel-a", 301_000L));
    }

    @Test public void historyStaysBounded() {
        RecentFingerprintCache cache = new RecentFingerprintCache(300_000L, 2);
        cache.remember("reel-a", 1_000L);
        cache.remember("reel-b", 2_000L);
        cache.remember("reel-c", 3_000L);

        assertEquals(2, cache.size());
        assertFalse(cache.contains("reel-a", 4_000L));
    }

    @Test public void historySurvivesProcessRecreation() {
        RecentFingerprintCache firstProcess = new RecentFingerprintCache(300_000L, 150);
        firstProcess.remember("reel-a", 10_000L);

        RecentFingerprintCache nextProcess = new RecentFingerprintCache(300_000L, 150);
        nextProcess.restore(firstProcess.serialize(), 11_000L);

        assertTrue(nextProcess.contains("reel-a", 12_000L));
    }

    @Test public void restoreDropsExpiredAndFutureEntries() {
        RecentFingerprintCache cache = new RecentFingerprintCache(300_000L, 150);
        cache.restore("expired,1000\nfuture,999999", 400_000L);

        assertEquals(0, cache.size());
    }
}
