package com.doomly.app.update;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;
import static com.doomly.app.update.ApkVerificationPolicy.Failure.*;

public class ApkVerificationPolicyTest {
    private final ApkVerificationPolicy.Identity installed = identity("com.doomly.app", "1.0.0", 100, Set.of("publisher"));

    private ApkVerificationPolicy.Identity identity(String name, String version, long code, Set<String> signers) {
        return new ApkVerificationPolicy.Identity(name, version, code, signers);
    }

    @Test public void matchingPublisherAndNewerBuildAreAccepted() {
        assertNull(ApkVerificationPolicy.verify(identity("com.doomly.app", "1.0.1", 101, Set.of("publisher")), installed, "1.0.1", 101));
    }

    @Test public void debugOrPlaySigningMismatchIsExplainedAndRejected() {
        assertEquals(SIGNER_MISMATCH, ApkVerificationPolicy.verify(
                identity("com.doomly.app", "1.0.1", 101, Set.of("other-key")), installed, "1.0.1", 101));
    }

    @Test public void sameBuildAndDowngradesAreExplainedAndRejected() {
        for (long code : new long[]{99, 100}) {
            assertEquals(NOT_NEWER, ApkVerificationPolicy.verify(
                    identity("com.doomly.app", "1.0.1", code, Set.of("publisher")), installed, "1.0.1", code));
        }
    }

    @Test public void publisherIdentityIsStillRequiredForLegacyReleases() {
        assertEquals(SIGNER_MISMATCH, ApkVerificationPolicy.verify(
                identity("com.doomly.app", "1.0.1", 101, Set.of("other-key")), installed, "1.0.1", 0));
    }

    @Test public void wrongPackageAndReleaseMetadataAreRejected() {
        assertEquals(PACKAGE_MISMATCH, ApkVerificationPolicy.verify(
                identity("other.app", "1.0.1", 101, Set.of("publisher")), installed, "1.0.1", 101));
        assertEquals(RELEASE_MISMATCH, ApkVerificationPolicy.verify(
                identity("com.doomly.app", "1.0.2", 101, Set.of("publisher")), installed, "1.0.1", 101));
        assertEquals(RELEASE_MISMATCH, ApkVerificationPolicy.verify(
                identity("com.doomly.app", "1.0.1", 102, Set.of("publisher")), installed, "1.0.1", 101));
    }

    @Test public void missingSigningInformationIsNotTreatedAsAMatch() {
        assertEquals(SIGNING_INFO_UNAVAILABLE, ApkVerificationPolicy.verify(
                identity("com.doomly.app", "1.0.1", 101, Set.of()), installed, "1.0.1", 101));
    }
}
