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
}
