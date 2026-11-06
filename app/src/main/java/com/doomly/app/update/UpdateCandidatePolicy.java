package com.doomly.app.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Build codes permit forward upgrades between stable and beta version names. */
public final class UpdateCandidatePolicy {
    private static final Pattern CODE = Pattern.compile("(?m)^Build code: ([0-9]+)\\s*$");
    private UpdateCandidatePolicy() {}
    public static long buildCode(String notes) {
        Matcher match = CODE.matcher(notes == null ? "" : notes);
        if (!match.find()) return 0;
        try {
            long code = Long.parseLong(match.group(1));
            return code > 0 && code <= 2100000000L ? code : 0;
        } catch (NumberFormatException e) { return 0; }
    }
    public static boolean isNewer(String notes, String version, long installedCode, String installedVersion) {
        long code = buildCode(notes);
        return code > 0 ? code > installedCode : UpdateVersionPolicy.isNewer(version, installedVersion);
    }
}
