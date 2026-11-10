package com.doomly.app;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, time-based history used to avoid counting a revisited Reel twice. */
final class RecentFingerprintCache {
    private final long ttlMs;
    private final int maxEntries;
    private final LinkedHashMap<String, Long> entries = new LinkedHashMap<>();

    RecentFingerprintCache(long ttlMs, int maxEntries) {
        this.ttlMs = ttlMs;
        this.maxEntries = maxEntries;
    }

    boolean contains(String fingerprint, long now) {
        prune(now);
        Long countedAt = entries.get(fingerprint);
        return countedAt != null && now - countedAt < ttlMs;
    }

    void remember(String fingerprint, long now) {
        prune(now);
        entries.remove(fingerprint);
        entries.put(fingerprint, now);
        while (entries.size() > maxEntries) {
            Iterator<String> iterator = entries.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    int size() {
        return entries.size();
    }

    String serialize() {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<String, Long> entry : entries.entrySet()) {
            if (value.length() > 0) value.append('\n');
            value.append(entry.getKey()).append(',').append(entry.getValue());
        }
        return value.toString();
    }

    void restore(String serialized, long now) {
        entries.clear();
        if (serialized == null || serialized.isEmpty()) return;
        for (String line : serialized.split("\\n")) {
            String[] parts = line.split(",", 2);
            if (parts.length != 2 || parts[0].isEmpty()) continue;
            try {
                long timestamp = Long.parseLong(parts[1]);
                // Reject future values as well as expired values. This keeps a
                // device clock correction from making a Reel permanently sticky.
                if (timestamp <= now && now - timestamp < ttlMs) {
                    entries.put(parts[0], timestamp);
                }
            } catch (NumberFormatException ignored) {
                // A corrupt diagnostic entry should never affect counting.
            }
        }
        while (entries.size() > maxEntries) {
            Iterator<String> iterator = entries.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue() >= ttlMs) iterator.remove();
        }
    }
}
