package com.doomly.app.update;

import java.util.HashSet;
import java.util.Set;

public final class ApkVerificationPolicy {
    private ApkVerificationPolicy() {}

    public enum Failure {
        FILE_MISSING("The downloaded APK is missing. Retry the download."),
        SIZE_MISMATCH("The downloaded APK has the wrong size. Retry the download."),
        UNREADABLE_ARCHIVE("Android couldn't read the downloaded APK. Retry the download."),
        PACKAGE_MISMATCH("The downloaded APK isn't Doomly. Nothing was installed."),
        RELEASE_MISMATCH("The APK doesn't match this release's version. Nothing was installed."),
        NOT_NEWER("This APK is the same build or older than the installed app. Check for a newer release."),
        SIGNING_INFO_UNAVAILABLE("Android couldn't read the app's signing information. Nothing was installed."),
        SIGNER_MISMATCH("The APK's signing key doesn't match this installation. Nothing was installed.");

        public final String message;
        Failure(String message) { this.message = message; }
    }

    public static final class Identity {
        final String packageName;
        final String versionName;
        final long versionCode;
        final Set<String> signers;

        public Identity(String packageName, String versionName, long versionCode, Set<String> signers) {
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.signers = new HashSet<>(signers);
        }
    }

    public static Failure verify(Identity archive, Identity installed, String expectedVersion, long expectedCode) {
        if (!installed.packageName.equals(archive.packageName)) return Failure.PACKAGE_MISMATCH;
        if (!expectedVersion.equals(archive.versionName)
                || (expectedCode > 0 && archive.versionCode != expectedCode)) return Failure.RELEASE_MISMATCH;
        if (archive.versionCode <= installed.versionCode) return Failure.NOT_NEWER;
        if (installed.signers.isEmpty() || archive.signers.isEmpty()) return Failure.SIGNING_INFO_UNAVAILABLE;
        return installed.signers.equals(archive.signers) ? null : Failure.SIGNER_MISMATCH;
    }
}
