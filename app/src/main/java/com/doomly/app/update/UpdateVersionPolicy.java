package com.doomly.app.update;

/** Strict semantic-version comparison used by the GitHub updater. */
public final class UpdateVersionPolicy {
    private UpdateVersionPolicy() {}

    public static boolean isValid(String value) {
        return parse(value) != null;
    }

    public static boolean isNewer(String candidate, String current) {
        int[] next = parse(candidate);
        int[] now = parse(current);
        if (next == null || now == null) return false;
        for (int index = 0; index < 3; index++) {
            if (next[index] != now[index]) return next[index] > now[index];
        }
        return false;
    }

    private static int[] parse(String value) {
        if (value == null) return null;
        String stable = value.split("[-+]", 2)[0];
        String[] parts = stable.split("\\.", -1);
        if (parts.length != 3) return null;
        int[] parsed = new int[3];
        try {
            for (int index = 0; index < 3; index++) {
                if (parts[index].isEmpty() || !parts[index].matches("[0-9]+")) return null;
                parsed[index] = Integer.parseInt(parts[index]);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return parsed;
    }
}
