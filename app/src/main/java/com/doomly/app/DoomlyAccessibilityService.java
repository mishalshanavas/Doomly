package com.doomly.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.doomly.app.doomstats.RecentActivityLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Detects settled Instagram Reel views from accessibility events.
 *
 * The detector deliberately requires Instagram's dedicated clips viewer/media
 * structure. Generic Like/Comment/Share controls are not sufficient because
 * they also appear in the home feed and in Reel preview cards.
 */
@SuppressWarnings("deprecation")
public class DoomlyAccessibilityService extends AccessibilityService {

    private static final String TAG = "DoomlyDetector";
    private static final String INSTAGRAM_PKG = "com.instagram.android";
    private static final long INITIAL_VIEW_SETTLE_MS = 900L;
    private static final long SWIPE_SETTLE_MS = 500L;
    private static final long MIN_MS_BETWEEN_COUNTS = 350L;
    private static final long SAME_REEL_IGNORE_MS = 5L * 60_000L;
    private static final int MAX_RECENT_FINGERPRINTS = 150;
    private static final long REELS_EXIT_GRACE_MS = 3_000L;
    private static final long AD_LOG_THROTTLE_MS = 2_000L;
    private static final long MAX_LOG_BYTES = 256L * 1024L;
    private static final String DETECTOR_PREFS = "doomly_detector";
    private static final String KEY_RECENT_FINGERPRINTS = "recent_fingerprints";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingCount;
    private String lastObservedFingerprint = "";
    private final RecentFingerprintCache recentlyCounted =
            new RecentFingerprintCache(SAME_REEL_IGNORE_MS, MAX_RECENT_FINGERPRINTS);
    private long lastCountAtMs;
    private long reelsSeenAtMs;
    private long lastAdLogAtMs;
    private boolean inReels;
    private String lastScreenSummary = "";
    private boolean fileLogEnabled;
    private File logFile;

    @Override
    public void onCreate() {
        super.onCreate();
        recentlyCounted.restore(
                getSharedPreferences(DETECTOR_PREFS, MODE_PRIVATE)
                        .getString(KEY_RECENT_FINGERPRINTS, ""),
                System.currentTimeMillis());
        fileLogEnabled = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (fileLogEnabled) {
            File external = getExternalFilesDir(null);
            if (external != null) {
                File directory = new File(external, "logs");
                if (directory.exists() || directory.mkdirs()) {
                    logFile = new File(directory, "reel-detector.log");
                    rotateLogIfNeeded();
                }
            }
        }
        fileLog("=== detector started ===");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null
                || !INSTAGRAM_PKG.contentEquals(event.getPackageName())) return;

        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_VIEW_SCROLLED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            long now = SystemClock.elapsedRealtime();
            ScreenInfo info = ScreenInfo.from(root);
            String summary = info.summary();
            if (!summary.equals(lastScreenSummary)) {
                lastScreenSummary = summary;
                fileLog("screen " + summary);
            }

            if (!info.looksLikeReels()) {
                if (inReels && now - reelsSeenAtMs > REELS_EXIT_GRACE_MS) leaveReels();
                return;
            }
            reelsSeenAtMs = now;

            boolean enteredNow = false;
            if (!inReels) {
                inReels = true;
                enteredNow = true;
                lastObservedFingerprint = info.contentFingerprint;
                debug("entered reels");
                fileLog(">>> entered reels");
                if (!info.isOverlayOpen && !info.isAd) {
                    scheduleSettledCount(INITIAL_VIEW_SETTLE_MS, "initial");
                }
            }

            if (info.isOverlayOpen || info.isAd) {
                cancelPendingCount();
                if (info.isAd && now - lastAdLogAtMs >= AD_LOG_THROTTLE_MS) {
                    lastAdLogAtMs = now;
                    fileLog("skip ad");
                }
                return;
            }

            // Instagram 445 commonly reports a Reel transition as content
            // changes without a TYPE_VIEW_SCROLLED event. The fingerprint is
            // built only from stable media/author/caption nodes, so dynamic
            // like counts and playback controls cannot trigger this path.
            if (!enteredNow && !info.contentFingerprint.isEmpty()
                    && !info.contentFingerprint.equals(lastObservedFingerprint)) {
                lastObservedFingerprint = info.contentFingerprint;
                fileLog("candidate content-change fingerprint=" + info.contentFingerprint);
                scheduleSettledCount(SWIPE_SETTLE_MS, "content-change");
            }

            if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED
                    && isReelPagerScroll(event, info)) {
                fileLog("reel scroll from=" + event.getFromIndex() + " to=" + event.getToIndex());
                scheduleSettledCount(SWIPE_SETTLE_MS, "swipe");
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Reel detector event failed", error);
            fileLog("event error " + error.getClass().getSimpleName());
        } finally {
            root.recycle();
        }
    }

    private boolean isReelPagerScroll(AccessibilityEvent event, ScreenInfo info) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return info.hasViewerPager;
        try {
            String id = lower(source.getViewIdResourceName());
            String className = lower(source.getClassName());
            return ReelDetectionPolicy.isReelPagerScroll(id, className, info.hasViewerPager);
        } finally {
            source.recycle();
        }
    }

    private void scheduleSettledCount(long delayMs, String reason) {
        cancelPendingCount();
        pendingCount = () -> countSettledReel(reason);
        handler.postDelayed(pendingCount, delayMs);
    }

    private void countSettledReel(String reason) {
        pendingCount = null;
        if (!inReels) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            ScreenInfo info = ScreenInfo.from(root);
            long now = SystemClock.elapsedRealtime();
            if (!info.looksLikeReels() || info.isOverlayOpen || info.isAd) return;
            String fingerprint = info.contentFingerprint;
            if (fingerprint.isEmpty()) {
                fileLog("skip no fingerprint");
                return;
            }
            if (now - lastCountAtMs < MIN_MS_BETWEEN_COUNTS) return;
            long wallClockNow = System.currentTimeMillis();
            if (recentlyCounted.contains(fingerprint, wallClockNow)) {
                fileLog("dedup fingerprint=" + fingerprint);
                return;
            }

            DoomStatsStore.Snapshot snapshot = DoomStatsStore.recordReel(this);
            RecentActivityLog.record(this, 1);
            MotivationNotifier.celebrateIfNeeded(this, snapshot);
            recentlyCounted.remember(fingerprint, wallClockNow);
            getSharedPreferences(DETECTOR_PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_RECENT_FINGERPRINTS, recentlyCounted.serialize())
                    .apply();
            lastCountAtMs = now;
            String message = "counted reason=" + reason + " fingerprint=" + fingerprint
                    + " today=" + snapshot.todayReels + " total=" + snapshot.totalReels;
            debug(message);
            fileLog(message);
            CloudSyncScheduler.scheduleSync(this);
            sendStatsBroadcast();
        } finally {
            root.recycle();
        }
    }

    private void cancelPendingCount() {
        if (pendingCount != null) {
            handler.removeCallbacks(pendingCount);
            pendingCount = null;
        }
    }

    private void leaveReels() {
        cancelPendingCount();
        if (inReels) {
            debug("left reels");
            fileLog("<<< left reels");
        }
        inReels = false;
    }

    @Override
    public void onInterrupt() {
        leaveReels();
    }

    @Override
    public void onDestroy() {
        leaveReels();
        handler.removeCallbacksAndMessages(null);
        fileLog("=== detector stopped ===");
        super.onDestroy();
    }

    private void debug(String line) {
        if (fileLogEnabled) Log.d(TAG, line);
        sendStatsBroadcast();
    }

    private void sendStatsBroadcast() {
        sendBroadcast(new Intent(DoomStatsStore.ACTION_STATS_CHANGED).setPackage(getPackageName()));
    }

    private void rotateLogIfNeeded() {
        if (logFile != null && logFile.length() > MAX_LOG_BYTES) {
            File old = new File(logFile.getParentFile(), "reel-detector.previous.log");
            if (old.exists()) old.delete();
            if (!logFile.renameTo(old)) logFile.delete();
        }
    }

    private void fileLog(String message) {
        if (!fileLogEnabled || logFile == null) return;
        rotateLogIfNeeded();
        try (FileWriter writer = new FileWriter(logFile, true)) {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date());
            writer.write(timestamp + " " + message + "\n");
        } catch (IOException ignored) {
            // Debug logging must never affect counting.
        }
    }

    private static String lower(CharSequence value) {
        return value == null ? "" : value.toString().toLowerCase(Locale.US);
    }

    private static final class ScreenInfo {
        final String rootClass;
        final boolean hasScrollable;
        final int controlCount;
        final boolean hasReelDescription;
        final boolean hasViewerPager;
        final boolean hasMediaComponent;
        final boolean isReelsTabSelected;
        final boolean hasFeedPreview;
        final boolean isOverlayOpen;
        final boolean isAd;
        final String contentFingerprint;

        ScreenInfo(Analyzer analyzer, String rootClass) {
            this.rootClass = rootClass;
            hasScrollable = analyzer.hasScrollable;
            controlCount = (analyzer.hasLike ? 1 : 0)
                    + (analyzer.hasComment ? 1 : 0)
                    + (analyzer.hasShare ? 1 : 0);
            hasReelDescription = analyzer.hasReelDescription;
            hasViewerPager = analyzer.hasViewerPager;
            hasMediaComponent = analyzer.hasMediaComponent;
            isReelsTabSelected = analyzer.isReelsTabSelected;
            hasFeedPreview = analyzer.hasFeedPreview;
            isOverlayOpen = analyzer.isOverlayOpen;
            isAd = analyzer.isAd;
            contentFingerprint = fingerprint(analyzer.fingerprintMaterial());
        }

        static ScreenInfo from(AccessibilityNodeInfo root) {
            Analyzer analyzer = new Analyzer(root);
            analyzer.scan(root, 0);
            return new ScreenInfo(analyzer, lower(root.getClassName()));
        }

        boolean looksLikeReels() {
            return ReelDetectionPolicy.looksLikeReels(
                    hasViewerPager, hasMediaComponent, isReelsTabSelected,
                    hasFeedPreview, controlCount, hasReelDescription);
        }

        String summary() {
            return "viewer=" + hasViewerPager + " media=" + hasMediaComponent
                    + " tab=" + isReelsTabSelected + " preview=" + hasFeedPreview
                    + " controls=" + controlCount + " desc=" + hasReelDescription
                    + " overlay=" + isOverlayOpen + " ad=" + isAd
                    + " scrollable=" + hasScrollable + " root=" + rootClass;
        }

        private static String fingerprint(String raw) {
            return raw.isEmpty() ? "" : Integer.toHexString(raw.hashCode());
        }
    }

    private static final class Analyzer {
        boolean hasScrollable;
        boolean hasLike;
        boolean hasComment;
        boolean hasShare;
        boolean hasReelDescription;
        boolean hasViewerPager;
        boolean hasMediaComponent;
        boolean isReelsTabSelected;
        boolean hasFeedPreview;
        boolean isOverlayOpen;
        boolean isAd;
        final int screenWidth;
        final int screenHeight;
        final int screenCenterY;
        String primaryMedia = "";
        String primaryAuthor = "";
        String primaryCaption = "";
        int primaryMediaScore = Integer.MIN_VALUE;
        int primaryAuthorScore = Integer.MIN_VALUE;
        int primaryCaptionScore = Integer.MIN_VALUE;

        Analyzer(AccessibilityNodeInfo root) {
            Rect bounds = new Rect();
            root.getBoundsInScreen(bounds);
            screenWidth = Math.max(1, bounds.width());
            screenHeight = Math.max(1, bounds.height());
            screenCenterY = bounds.centerY();
        }

        void scan(AccessibilityNodeInfo node, int depth) {
            if (node == null || depth > 18) return;
            String id = lower(node.getViewIdResourceName());
            String description = lower(node.getContentDescription());
            String text = lower(node.getText());
            String combined = description + " " + text;
            boolean visible = node.isVisibleToUser();
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            hasScrollable |= visible && node.isScrollable();
            hasViewerPager |= visible && id.endsWith("/clips_viewer_view_pager");
            hasMediaComponent |= visible && (id.endsWith("/clips_media_component")
                    || id.endsWith("/clips_video_container"));
            isReelsTabSelected |= visible && id.endsWith("/clips_tab") && node.isSelected();
            hasFeedPreview |= visible && (id.contains("feed_preview") || id.contains("row_feed_"));

            hasLike |= visible && (id.endsWith("/like_button") || "like".equals(description));
            hasComment |= visible && (id.endsWith("/comment_button") || "comment".equals(description));
            hasShare |= visible && (id.endsWith("/direct_share_button") || "share".equals(description));
            hasReelDescription |= visible && (description.startsWith("reel by ")
                    || description.contains("reels video"));

            isOverlayOpen |= visible && (id.contains("bottom_sheet")
                    || id.contains("comment_composer") || id.contains("direct_share_sheet")
                    || combined.contains("write a message")
                    || combined.contains("search users") || combined.contains("replying to"));
            isAd |= visible && (combined.contains("sponsored") || combined.contains("ad choices")
                    || combined.contains("about this ad") || combined.contains("paid partnership")
                    || combined.contains("paid promotion"));

            if (visible) collectFingerprintPart(id, description, text, node, bounds);

            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) continue;
                try {
                    scan(child, depth + 1);
                } finally {
                    child.recycle();
                }
            }
        }

        String fingerprintMaterial() {
            if (primaryMedia.isEmpty() && primaryAuthor.isEmpty()) return "";
            return primaryMedia + "|" + primaryAuthor + "|" + primaryCaption;
        }

        private void collectFingerprintPart(
                String id, String description, String text,
                AccessibilityNodeInfo node, Rect bounds) {
            int centeredScore = bounds.contains(screenWidth / 2, screenCenterY) ? 1_000_000 : 0;
            int score = centeredScore + Math.max(0, bounds.width() * bounds.height() / 1000)
                    - Math.abs(bounds.centerY() - screenCenterY);

            if (id.endsWith("/clips_media_component") && !description.isEmpty()
                    && score > primaryMediaScore) {
                primaryMediaScore = score;
                primaryMedia = clean(description);
            }
            if (id.endsWith("/clips_author_username") && score > primaryAuthorScore) {
                String author = !text.isEmpty() ? text : description;
                if (!author.isEmpty()) {
                    primaryAuthorScore = score;
                    primaryAuthor = clean(author);
                }
            }

            // Instagram 445 exposes the visible caption without a resource id.
            // It distinguishes consecutive Reels from the same creator.
            boolean captionRegion = bounds.top > screenHeight * 0.55f
                    && bounds.bottom < screenHeight * 0.94f
                    && bounds.left < screenWidth * 0.2f
                    && bounds.right < screenWidth * 0.94f;
            if (id.isEmpty() && captionRegion && !node.isClickable()
                    && description.length() >= 4 && score > primaryCaptionScore) {
                primaryCaptionScore = score;
                primaryCaption = clean(description);
            }
        }

        private static String clean(String value) {
            String cleaned = value.replaceAll("[^\\p{L}\\p{N}._ ]", " ")
                    .replaceAll("\\s+", " ").trim();
            return cleaned.length() > 180 ? cleaned.substring(0, 180) : cleaned;
        }
    }
}
