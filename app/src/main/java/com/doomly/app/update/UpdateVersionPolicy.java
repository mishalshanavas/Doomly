package com.doomly.app.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict semantic-version comparison used by the GitHub updater. */
public final class UpdateVersionPolicy {
    private static final Pattern VERSION = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?");
    private UpdateVersionPolicy() {}

    public static boolean isValid(String value) {
        return parse(value) != null;
    }

    public static boolean isNewer(String candidate, String current) {
        Matcher next = parse(candidate);
        Matcher now = parse(current);
        if (next == null || now == null) return false;
        for (int index = 1; index <= 3; index++) {
            int comparison = compareNumeric(next.group(index), now.group(index));
            if (comparison != 0) return comparison > 0;
        }
        String nextPre = next.group(4), nowPre = now.group(4);
        if (nextPre == null || nowPre == null) return nextPre == null && nowPre != null;
        String[] nextParts = nextPre.split("\\."), nowParts = nowPre.split("\\.");
        for (int index = 0; index < Math.min(nextParts.length, nowParts.length); index++) {
            String a = nextParts[index], b = nowParts[index];
            boolean numericA = a.matches("[0-9]+"), numericB = b.matches("[0-9]+");
            int comparison = numericA && numericB ? compareNumeric(a, b)
                    : numericA != numericB ? (numericA ? -1 : 1) : a.compareTo(b);
            if (comparison != 0) return comparison > 0;
        }
        return nextParts.length > nowParts.length;
    }

    private static int compareNumeric(String a, String b) {
        return a.length() != b.length() ? Integer.compare(a.length(), b.length()) : a.compareTo(b);
    }

    private static Matcher parse(String value) {
        if (value == null) return null;
        Matcher match = VERSION.matcher(value);
        if (!match.matches()) return null;
        String prerelease = match.group(4);
        if (prerelease != null) {
            for (String part : prerelease.split("\\.")) {
                if (part.matches("[0-9]+") && part.length() > 1 && part.startsWith("0")) return null;
            }
        }
        return match;
    }
}
