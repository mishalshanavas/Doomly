package com.doomly.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.doomly.app.doomstats.RecentActivityLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Scroll-driven reel detector with content-fingerprint deduplication.
 *
 * Detection: UI-pattern recognition (like+comment+share buttons, audio label,
 * reel content descriptions) — no dependency on class names or CollectionInfo.
 *
 * Counting: TYPE_VIEW_SCROLLED triggers a candidate count. A lightweight
 * content fingerprint (first ~300 chars of content descriptions) prevents
 * double-counting the same reel during content refreshes. Fingerprints expire
 * after 12s so re-watching a reel counts again.
 *
 * Debug logs: /sdcard/Android/data/com.doomly.app/files/logs/debug.log
 */
@SuppressWarnings("deprecation")
public class DoomlyAccessibilityService extends AccessibilityService {

    private static final String INSTAGRAM_PKG = "com.instagram.android";

    // ── Timing ─────────────────────────────────────────────────────────
    private static final long MIN_MS_BETWEEN_REELS = 800L;    // minimum gap between counts
    private static final long SAME_FP_IGNORE_MS = 12_000L;    // ignore same fingerprint for 12s
    private static final long REELS_EXIT_GRACE_MS = 3_000L;
    private static final long DEBUG_THROTTLE_MS = 500L;

    // ── Dedup state ────────────────────────────────────────────────────
    private String lastCountedFp = "";       // fingerprint of last counted reel
    private long lastCountedFpAtMs = 0L;     // when that fingerprint was counted
    private long lastCountAtMs = 0L;
    private long lastScrollEventAtMs = 0L;
    private long reelsSeenAtMs = 0L;
    private long lastDebugAtMs = 0L;
    private boolean inReels = false;
    private String lastScreenSummary = "";

    // ── File logging ───────────────────────────────────────────────────
    private static final boolean FILE_LOG_ENABLED = true;
    private static File logFile = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (FILE_LOG_ENABLED) {
            try {
                File dir = new File(getExternalFilesDir(null), "logs");
                if (!dir.exists()) dir.mkdirs();
                logFile = new File(dir, "debug.log");
            } catch (Exception ignored) {}
        }
        fileLog("=== Service created ===");
    }

    // ── AccessibilityService lifecycle ─────────────────────────────────

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!INSTAGRAM_PKG.contentEquals(event.getPackageName())) return;

        int type = event.getEventType();

        // Accept all event types that might carry scroll or content info
        if (type != AccessibilityEvent.TYPE_VIEW_SCROLLED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_VIEW_SELECTED) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            long now = SystemClock.elapsedRealtime();

            // ── 1. Detect Reels screen via UI pattern ─────────────────
            ScreenInfo info = ScreenInfo.from(root);

            // Build summary for logging
            String summary = "cls=" + info.rootClass
                    + " scroll=" + info.hasScrollable
                    + " like=" + info.hasLikeButton
                    + " cmt=" + info.hasCommentButton
                    + " share=" + info.hasShareButton
                    + " audio=" + info.hasAudioLabel
                    + " reelDesc=" + info.hasReelDescription
                    + " reelId=" + info.hasReelResourceId
                    + " overlay=" + info.isOverlayOpen
                    + " ad=" + info.isAd;

            // Log if significant change
            if (!summary.equals(lastScreenSummary)) {
                lastScreenSummary = summary;
                fileLog("screen: " + summary);
            }

            boolean looksLikeReels = info.looksLikeReels();

            if (!looksLikeReels) {
                if (inReels && (now - reelsSeenAtMs) > REELS_EXIT_GRACE_MS) {
                    leaveReels(now);
                }
                if (!inReels) {
                    if ((now - lastDebugAtMs) > 5000L) {
                        fileLog("not-reels: " + summary);
                        lastDebugAtMs = now;
                    }
                }
                return;
            }
            reelsSeenAtMs = now;

            if (info.isAd) {
                fileLog("skip: ad detected");
                return;
            }

            // Overlay open (comments sheet, share sheet) — stay in reels
            // but don't count (scrolling a text field isn't a reel swipe)
            if (info.isOverlayOpen) {
                return;
            }

            if (!inReels) {
                inReels = true;
                lastCountedFp = "";
                fileLog(">>> ENTERED REELS <<< " + summary);
                debug("entered reels", true);
            }

            // ── 2. Count on scroll with fingerprint dedup ─────────────
            if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                lastScrollEventAtMs = now;

                if ((now - lastCountAtMs) < MIN_MS_BETWEEN_REELS) return;

                // Build a lightweight fingerprint of current reel content
                String fp = info.contentFingerprint;
                if (fp.isEmpty()) return;

                // Same fingerprint as last counted? Skip (content refresh,
                // not a new reel) — unless enough time has passed
                if (fp.equals(lastCountedFp)
                        && (now - lastCountedFpAtMs) < SAME_FP_IGNORE_MS) {
                    fileLog("dedup: same fp=" + fp.substring(0, Math.min(8, fp.length())));
                    return;
                }

                countReel(now, "scroll", fp);
            }

            // ── 3. Fallback: count on TYPE_VIEW_SELECTED ──────────────
            if (type == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                if ((now - lastCountAtMs) >= MIN_MS_BETWEEN_REELS
                        && (now - lastScrollEventAtMs) < 5000L) {
                    String fp = info.contentFingerprint;
                    if (!fp.isEmpty()
                            && !fp.equals(lastCountedFp)) {
                        countReel(now, "selected", fp);
                    }
                }
            }

        } catch (Exception e) {
            fileLog("ERROR: " + stackTraceToString(e));
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        fileLog("onInterrupt");
        leaveReels(SystemClock.elapsedRealtime());
    }

    @Override
    public void onDestroy() {
        fileLog("=== Service destroyed ===");
        leaveReels(SystemClock.elapsedRealtime());
        super.onDestroy();
    }

    // ── Counting ───────────────────────────────────────────────────────

    private void countReel(long now, String reason, String fp) {
        DoomStatsStore.Snapshot snapshot = DoomStatsStore.recordReel(this);
        RecentActivityLog.record(this, 1);
        MotivationNotifier.celebrateIfNeeded(this, snapshot);
        lastCountedFp = fp;
        lastCountedFpAtMs = now;
        lastCountAtMs = now;
        String shortFp = fp.substring(0, Math.min(8, fp.length()));
        String msg = "COUNTED [" + reason + "] fp=" + shortFp
                + " total=" + snapshot.totalReels
                + " today=" + snapshot.todayReels;
        debug(msg, true);
        fileLog(msg);
        CloudSyncScheduler.scheduleSync(this);
        sendStatsBroadcast();
    }

    // ── State management ───────────────────────────────────────────────

    private void leaveReels(long now) {
        if (inReels) {
            fileLog("<<< LEFT REELS >>>");
            debug("left reels", false);
        }
        inReels = false;
        lastCountedFp = "";
    }

    // ── Debug / broadcast ──────────────────────────────────────────────

    private void debug(String line, boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && (now - lastDebugAtMs) < DEBUG_THROTTLE_MS) return;
        lastDebugAtMs = now;
        DoomStatsStore.recordDebug(this, line);
        sendStatsBroadcast();
    }

    private void sendStatsBroadcast() {
        sendBroadcast(new Intent(DoomStatsStore.ACTION_STATS_CHANGED)
                .setPackage(getPackageName()));
    }

    // ── File logging ───────────────────────────────────────────────────

    private void fileLog(String msg) {
        if (!FILE_LOG_ENABLED || logFile == null) return;
        try {
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            FileWriter fw = new FileWriter(logFile, true);
            fw.write(timestamp + " " + msg + "\n");
            fw.close();
        } catch (IOException ignored) {}
    }

    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // ── ScreenInfo: UI-pattern-based screen classification ─────────────

    /**
     * Classifies the current screen by looking for the distinctive
     * Reels UI pattern: a vertical scrollable container that has
     * like, comment, and share buttons as siblings/descendants.
     *
     * This works regardless of class name obfuscation because it
     * relies on content descriptions and accessibility actions,
     * which must remain readable for accessibility compliance.
     */
    private static final class ScreenInfo {
        final String rootClass;
        final boolean hasScrollable;
        final boolean hasLikeButton;
        final boolean hasCommentButton;
        final boolean hasShareButton;
        final boolean hasAudioLabel;
        final boolean hasReelDescription;
        final boolean hasReelResourceId;
        final boolean isOverlayOpen;
        final boolean isAd;
        final String contentFingerprint;   // lightweight hash of reel content

        ScreenInfo(String rootClass, boolean hasScrollable,
                   boolean hasLikeButton, boolean hasCommentButton,
                   boolean hasShareButton,
                   boolean hasAudioLabel, boolean hasReelDescription,
                   boolean hasReelResourceId,
                   boolean isOverlayOpen, boolean isAd,
                   String contentFingerprint) {
            this.rootClass = rootClass;
            this.hasScrollable = hasScrollable;
            this.hasLikeButton = hasLikeButton;
            this.hasCommentButton = hasCommentButton;
            this.hasShareButton = hasShareButton;
            this.hasAudioLabel = hasAudioLabel;
            this.hasReelDescription = hasReelDescription;
            this.hasReelResourceId = hasReelResourceId;
            this.isOverlayOpen = isOverlayOpen;
            this.isAd = isAd;
            this.contentFingerprint = contentFingerprint;
        }

        /** True if the current screen matches the Reels viewer pattern. */
        boolean looksLikeReels() {
            // Must have at least 1 of the 3 Reels controls (lower bar when
            // already in reels — Instagram sometimes hides buttons during
            // transitions; stickiness prevents premature exit)
            int controls = (hasLikeButton ? 1 : 0)
                    + (hasCommentButton ? 1 : 0)
                    + (hasShareButton ? 1 : 0);

            // Strong signals: audio label, reel content description, or
            // resource IDs containing reel/clips identifiers
            boolean hasReelContent = hasAudioLabel || hasReelDescription || hasReelResourceId;

            // Require controls OR reel-specific content (not both)
            if (controls == 0 && !hasReelContent) return false;

            // Must have a scrollable container or ViewPager root
            if (!hasScrollable && !rootClass.contains("ViewPager")
                    && !rootClass.contains("RecyclerView")
                    && !rootClass.contains("ViewPager2")) {
                // If we have strong reel content signal, allow without scrollable
                if (!hasReelContent) return false;
            }

            return true;
        }

        static ScreenInfo from(AccessibilityNodeInfo root) {
            String rootClass = root.getClassName() != null
                    ? root.getClassName().toString() : "";

            boolean[] hasScrollable = {false};
            boolean[] hasLike = {false};
            boolean[] hasComment = {false};
            boolean[] hasShare = {false};
            boolean[] hasAudio = {false};
            boolean[] hasReelDesc = {false};
            boolean[] hasReelId = {false};
            boolean[] isOverlay = {false};
            boolean[] isAd = {false};
            int[] depth = {0};

            // Collect content for fingerprinting (first ~300 chars of
            // content descriptions, excluding UI chrome words)
            StringBuilder fpBuilder = new StringBuilder();

            scan(root, hasScrollable, hasLike, hasComment, hasShare,
                    hasAudio, hasReelDesc, hasReelId,
                    isOverlay, isAd, depth, 16, fpBuilder);

            String fp = buildFingerprint(fpBuilder.toString());

            return new ScreenInfo(rootClass, hasScrollable[0],
                    hasLike[0], hasComment[0], hasShare[0],
                    hasAudio[0], hasReelDesc[0], hasReelId[0],
                    isOverlay[0], isAd[0], fp);
        }

        private static void scan(AccessibilityNodeInfo node,
                                  boolean[] scrollable,
                                  boolean[] like, boolean[] comment,
                                  boolean[] share,
                                  boolean[] audio, boolean[] reelDesc,
                                  boolean[] reelId,
                                  boolean[] overlay, boolean[] ad,
                                  int[] depth, int maxDepth,
                                  StringBuilder fpBuilder) {
            if (node == null || depth[0] > maxDepth) return;

            if (node.isScrollable()) scrollable[0] = true;

            String cd = node.getContentDescription() != null
                    ? node.getContentDescription().toString().toLowerCase(Locale.US)
                    : "";
            String text = node.getText() != null
                    ? node.getText().toString().toLowerCase(Locale.US)
                    : "";
            String resId = node.getViewIdResourceName() != null
                    ? node.getViewIdResourceName().toLowerCase(Locale.US)
                    : "";
            String combined = cd + " " + text;

            // ── Fingerprint collection (first ~300 chars) ────────────
            if (fpBuilder.length() < 300 && !cd.isEmpty()) {
                // Strip UI chrome words to keep fingerprint stable
                String cleaned = cd
                        .replaceAll("\\b(like|liked|comment|comments|share|send|more|reels|clips|home|search|profile|follow|following)\\b", "")
                        .replaceAll("\\b(view translation|original audio|audio|double tap to like|double tap to unlike)\\b", "")
                        .replaceAll("[^a-z0-9 ]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                if (!cleaned.isEmpty()) {
                    if (fpBuilder.length() > 0) fpBuilder.append('|');
                    fpBuilder.append(cleaned);
                    // Truncate if exceeds limit
                    if (fpBuilder.length() > 300) {
                        fpBuilder.setLength(300);
                    }
                }
            }

            // ── Like button ─────────────────────────────────────────
            if (!like[0]) {
                if (combined.contains("like")
                        || combined.contains("double tap to like")
                        || combined.contains("double tap to unlike")) {
                    like[0] = true;
                }
            }

            // ── Comment button ──────────────────────────────────────
            if (!comment[0]) {
                if (combined.contains("comment")
                        || combined.contains("add a comment")
                        || combined.contains("view comments")) {
                    comment[0] = true;
                }
            }

            // ── Share / Send button ─────────────────────────────────
            if (!share[0]) {
                if (combined.contains("share")
                        || combined.contains("send to")
                        || combined.contains("send")) {
                    share[0] = true;
                }
            }

            // ── Audio label (strong Reels signal) ───────────────────
            if (!audio[0]) {
                if (combined.contains("original audio")
                        || combined.contains("audio")
                        || cd.contains("audio")) {
                    audio[0] = true;
                }
            }

            // ── Reel content description ────────────────────────────
            if (!reelDesc[0]) {
                if (cd.contains("reel")
                        || cd.contains("video by")
                        || cd.contains("reels video")
                        || text.contains("reel")) {
                    reelDesc[0] = true;
                }
            }

            // ── Reel resource IDs ───────────────────────────────────
            if (!reelId[0]) {
                if (resId.contains("reel")
                        || resId.contains("clips")
                        || resId.contains("clip_viewer")) {
                    reelId[0] = true;
                }
            }

            // ── Overlay detection ───────────────────────────────────
            if (!overlay[0]) {
                if (combined.contains("add a comment")
                        || combined.contains("write a message")
                        || combined.contains("search users")
                        || combined.contains("not interested")
                        || combined.contains("report")) {
                    overlay[0] = true;
                }
            }

            // ── Ad detection ────────────────────────────────────────
            if (!ad[0]) {
                if (cd.contains("sponsored")
                        || cd.contains("ad choices")
                        || cd.contains("about this ad")
                        || cd.contains("paid partnership")
                        || cd.contains("paid promotion")) {
                    ad[0] = true;
                }
            }

            // Recurse children
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) continue;
                depth[0]++;
                try {
                    scan(child, scrollable, like, comment, share,
                            audio, reelDesc, reelId,
                            overlay, ad, depth, maxDepth, fpBuilder);
                } finally {
                    depth[0]--;
                    child.recycle();
                }
            }
        }

        /** Build a compact fingerprint from collected content descriptions. */
        private static String buildFingerprint(String raw) {
            if (raw.isEmpty()) return "";
            // Use Java's hashCode for speed — collisions are acceptable
            // since fingerprints expire after SAME_FP_IGNORE_MS
            return Integer.toHexString(raw.hashCode());
        }
    }
}
