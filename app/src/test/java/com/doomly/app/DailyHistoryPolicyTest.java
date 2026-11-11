package com.doomly.app;

import org.junit.Test;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class DailyHistoryPolicyTest {
    private final LocalDate today = LocalDate.of(2026, 11, 12);

    @Test public void restoredCountsNeverReplaceHigherLocalCounts() {
        Map<String, Integer> local = new HashMap<>();
        local.put("2026-11-11", 200);
        Map<String, Integer> cloud = new HashMap<>();
        cloud.put("2026-11-11", 100);
        cloud.put("2026-11-12", 50);
        Map<String, Integer> merged = DailyHistoryPolicy.merge(local, cloud, today);
        assertEquals(Integer.valueOf(200), merged.get("2026-11-11"));
        assertEquals(Integer.valueOf(50), merged.get("2026-11-12"));
        assertEquals(1, local.size());
    }

    @Test public void keepsAYearRegardlessOfInsertionOrder() {
        Map<String, Integer> history = new HashMap<>();
        for (int i = 0; i < 400; i++) history.put(today.minusDays(i).toString(), i);
        Map<String, Integer> merged = DailyHistoryPolicy.merge(history, new HashMap<>(), today);
        assertEquals(365, merged.size());
        assertTrue(merged.containsKey(today.minusDays(364).toString()));
        assertFalse(merged.containsKey(today.minusDays(365).toString()));
        assertEquals(today.minusDays(364).toString(), merged.keySet().iterator().next());
    }

    @Test public void rejectsInvalidFutureAndNegativeRows() {
        Map<String, Integer> history = new HashMap<>();
        history.put("not-a-date", 10);
        history.put(today.plusDays(1).toString(), 10);
        history.put(today.toString(), -1);
        assertTrue(DailyHistoryPolicy.merge(history, new HashMap<>(), today).isEmpty());
    }
}
