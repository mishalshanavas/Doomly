package com.doomly.app;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;

/** Date-ordered, bounded history; restoring a backup must never reduce a count. */
public final class DailyHistoryPolicy {
    public static final int RETENTION_DAYS = 365;

    private DailyHistoryPolicy() {}

    public static Map<String, Integer> merge(Map<String, Integer> local,
                                             Map<String, Integer> incoming,
                                             LocalDate today) {
        Map<String, Integer> result = new TreeMap<>();
        addRecent(result, local, today);
        addRecent(result, incoming, today);
        return result;
    }

    private static void addRecent(Map<String, Integer> result, Map<String, Integer> source,
                                  LocalDate today) {
        LocalDate first = today.minusDays(RETENTION_DAYS - 1);
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0) continue;
            try {
                LocalDate date = LocalDate.parse(entry.getKey());
                if (date.isBefore(first) || date.isAfter(today)) continue;
                result.merge(date.toString(), entry.getValue(), Math::max);
            } catch (DateTimeParseException ignored) {
                // Ignore malformed backup rows without discarding valid history.
            }
        }
    }
}
