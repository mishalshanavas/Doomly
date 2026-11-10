package com.doomly.app;

/** Pure decision rules kept separate so detector regressions can be unit tested. */
final class ReelDetectionPolicy {
    private ReelDetectionPolicy() {}

    static boolean looksLikeReels(
            boolean viewerPager,
            boolean mediaComponent,
            boolean reelsTabSelected,
            boolean feedPreview,
            int controlCount,
            boolean reelDescription) {
        // A visible feed preview wins over stale viewer nodes retained by
        // Instagram during navigation transitions. Count once the preview is gone.
        if (feedPreview) return false;
        boolean dedicatedViewer = viewerPager || mediaComponent;
        boolean enoughContent = controlCount >= 2 || reelDescription;
        return dedicatedViewer && enoughContent && (viewerPager || reelsTabSelected);
    }

    static boolean isReelPagerScroll(String sourceId, String sourceClass, boolean viewerPresent) {
        if (sourceId.contains("clips_viewer_view_pager")) return true;
        if (sourceId.contains("swipeable_tab_view_pager")) return false;
        return viewerPresent && sourceId.isEmpty() && sourceClass.contains("viewpager");
    }
}
