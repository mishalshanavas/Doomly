package com.doomly.app.update;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateVersionPolicyTest {
    @Test
    public void acceptsSemanticVersions() {
        assertTrue(UpdateVersionPolicy.isValid("1.0.0"));
        assertTrue(UpdateVersionPolicy.isValid("2.4.9-beta.1"));
        assertFalse(UpdateVersionPolicy.isValid("1.0"));
        assertFalse(UpdateVersionPolicy.isValid("latest"));
    }

    @Test
    public void comparesEachVersionComponent() {
        assertTrue(UpdateVersionPolicy.isNewer("1.0.1", "1.0.0"));
        assertTrue(UpdateVersionPolicy.isNewer("1.2.0", "1.1.99"));
        assertTrue(UpdateVersionPolicy.isNewer("2.0.0", "1.99.99"));
        assertFalse(UpdateVersionPolicy.isNewer("1.0.0", "1.0.0"));
        assertFalse(UpdateVersionPolicy.isNewer("0.9.9", "1.0.0"));
        assertFalse(UpdateVersionPolicy.isNewer("broken", "1.0.0"));
    }

    @Test public void stableReplacesSameVersionBeta() {
        assertTrue(UpdateVersionPolicy.isNewer("1.0.0", "1.0.0-beta"));
        assertFalse(UpdateVersionPolicy.isNewer("1.0.0-beta", "1.0.0"));
        assertTrue(UpdateVersionPolicy.isNewer("1.0.0-beta.10", "1.0.0-beta.2"));
        assertTrue(UpdateVersionPolicy.isNewer("1.0.0-rc.1", "1.0.0-beta.10"));
        assertFalse(UpdateVersionPolicy.isNewer("1.0.0+build.2", "1.0.0+build.1"));
    }

    @Test public void rejectsMalformedSemanticVersions() {
        assertFalse(UpdateVersionPolicy.isValid("1.0.0-"));
        assertFalse(UpdateVersionPolicy.isValid("1.0.0-beta!"));
        assertFalse(UpdateVersionPolicy.isValid("1.0.0-beta..1"));
        assertFalse(UpdateVersionPolicy.isValid("01.0.0"));
        assertFalse(UpdateVersionPolicy.isValid("1.0.0-01"));
    }
}
