package com.doomly.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReelDetectionPolicyTest {
    @Test public void fullViewerIsRecognized() {
        assertTrue(ReelDetectionPolicy.looksLikeReels(true, true, true, false, 3, true));
    }

    @Test public void feedPreviewIsRejected() {
        assertFalse(ReelDetectionPolicy.looksLikeReels(false, false, false, true, 3, true));
    }

    @Test public void feedPreviewRejectsStaleViewerNodes() {
        assertFalse(ReelDetectionPolicy.looksLikeReels(true, true, false, true, 3, true));
    }

    @Test public void viewerWorksWhenBottomTabSelectionIsNotReported() {
        assertTrue(ReelDetectionPolicy.looksLikeReels(true, true, false, false, 3, true));
    }

    @Test public void ordinaryFeedVideoIsRejected() {
        assertFalse(ReelDetectionPolicy.looksLikeReels(false, false, false, false, 3, false));
    }

    @Test public void horizontalTabScrollIsRejected() {
        assertFalse(ReelDetectionPolicy.isReelPagerScroll(
                "com.instagram.android:id/swipeable_tab_view_pager", "androidx.viewpager.widget.viewpager", true));
    }

    @Test public void clipsViewerScrollIsAccepted() {
        assertTrue(ReelDetectionPolicy.isReelPagerScroll(
                "com.instagram.android:id/clips_viewer_view_pager", "androidx.viewpager.widget.viewpager", true));
    }
}
