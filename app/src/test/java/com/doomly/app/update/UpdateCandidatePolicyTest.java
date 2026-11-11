package com.doomly.app.update;
import org.junit.Test;
import static org.junit.Assert.*;
public class UpdateCandidatePolicyTest {
    @Test public void choosesNewestLegacyReleaseNotFirstApiEntry() {
        assertTrue(UpdateCandidatePolicy.isPreferred("", "1.2.0", "", "1.1.0"));
        assertFalse(UpdateCandidatePolicy.isPreferred("Build code: 100", "9.0.0", "Build code: 101", "1.0.0"));
        assertTrue(UpdateCandidatePolicy.isPreferred("Build code: 101", "1.0.0", "Build code: 100", "9.0.0"));
        assertTrue(UpdateCandidatePolicy.isPreferred("", "1.0.0", "", "1.0.0-beta"));
    }
    @Test public void newerStableCanReplaceBeta() {
        assertTrue(UpdateCandidatePolicy.isNewer("Build code: 200\n\nRelease notes", "1.1.0", 199, "1.0.199-beta"));
        assertFalse(UpdateCandidatePolicy.isNewer("Build code: 198", "9.0.0", 199, "1.0.199-beta"));
        assertFalse(UpdateCandidatePolicy.isNewer("Build code: 199", "9.0.0", 199, "1.0.199-beta"));
    }
    @Test public void legacyAndInvalidMetadata() {
        assertTrue(UpdateCandidatePolicy.isNewer("Legacy", "1.2.0", 1, "1.1.0"));
        assertEquals(0, UpdateCandidatePolicy.buildCode("Build code: 999999999999999999999"));
        assertEquals(0, UpdateCandidatePolicy.buildCode("Build code: 2100000001"));
        assertEquals(0, UpdateCandidatePolicy.buildCode(null));
    }
}
