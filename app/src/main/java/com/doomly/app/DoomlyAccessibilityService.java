package com.doomly.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

public class DoomlyAccessibilityService extends AccessibilityService {
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final long MIN_MS_BETWEEN_REELS = 1700L;
    private static final long REEL_DWELL_MS = 1800L;
    private static final long SAME_FINGERPRINT_IGNORE_MS = 12_000L;
    private static final long DEBUG_THROTTLE_MS = 700L;
    private static final int MAX_NODE_PARTS = 90;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lastCountedFingerprint = "";
    private String lastCandidateFingerprint = "";
    private String lastLikedFingerprint = "";
    private String lastScrollToken = "";
    private long lastCandidateAtMs = 0L;
    private long lastCountAtMs = 0L;
    private long lastSameFingerprintCountAtMs = 0L;
    private long lastDebugAtMs = 0L;
    private long reelsEnteredAtMs = 0L;
    private boolean wasInReels = false;
    private boolean candidateStartedLiked = false;
    private Runnable pendingDwellRunnable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) {
            return;
        }
        if (!INSTAGRAM_PACKAGE.contentEquals(event.getPackageName())) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        try {
            Scene scene = Scene.from(root);
            if (!scene.looksLikeReelsScreen()) {
                debug("skip: not reels | " + scene.signalSummary(), true);
                resetReelsState();
                return;
            }

            if (scene.hasAdSignal()) {
                debug("skip: ad/sponsored reel | " + scene.signalSummary(), true);
                cancelPendingCandidate();
                lastCandidateFingerprint = "";
                return;
            }

            long now = SystemClock.elapsedRealtime();
            if (!wasInReels) {
                wasInReels = true;
                reelsEnteredAtMs = now;
                lastCandidateFingerprint = "";
                debug("entered reels | " + scene.signalSummary(), true);
            }

            String fingerprint = scene.reelFingerprint();
            if (fingerprint.length() < 12) {
                debug("skip: weak fingerprint | " + scene.signalSummary(), true);
                return;
            }

            if (lastCandidateFingerprint.isEmpty()) {
                startCandidate(fingerprint, now, "entry", scene);
                return;
            }

            if (isNewVerticalScroll(event, now)) {
                startCandidate(fingerprint, now, "scroll", scene);
                return;
            }

            if (!fingerprint.equals(lastCandidateFingerprint)) {
                debug("skip: content refresh only | fp=" + shortFingerprint(fingerprint), false);
                return;
            }

            maybeRecordLike(scene, fingerprint);

            long dwellMs = now - lastCandidateAtMs;
            if (dwellMs >= REEL_DWELL_MS && canCount(now, fingerprint)) {
                recordReel(fingerprint, now, "counted: dwell " + dwellMs + "ms");
            } else {
                debug("waiting: dwell " + dwellMs + "/" + REEL_DWELL_MS + "ms | fp=" + shortFingerprint(fingerprint), false);
            }
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        resetReelsState();
    }

    private boolean isNewVerticalScroll(AccessibilityEvent event, long now) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            return false;
        }
        if (now - reelsEnteredAtMs < 500L) {
            return false;
        }

        String token = event.getScrollY() + ":"
                + event.getFromIndex() + ":"
                + event.getToIndex() + ":"
                + event.getItemCount();
        if (token.equals(lastScrollToken)) {
            return false;
        }
        lastScrollToken = token;

        return event.getScrollY() != -1
                || event.getFromIndex() != -1
                || event.getToIndex() != -1
                || event.getItemCount() > 0;
    }

    private boolean canCount(long now, String fingerprint) {
        boolean enoughTimePassed = now - lastCountAtMs >= MIN_MS_BETWEEN_REELS;
        if (!enoughTimePassed) {
            return false;
        }

        boolean isNewFingerprint = !fingerprint.equals(lastCountedFingerprint);
        boolean sameFingerprintExpired = now - lastSameFingerprintCountAtMs >= SAME_FINGERPRINT_IGNORE_MS;
        return isNewFingerprint || sameFingerprintExpired;
    }

    private void startCandidate(String fingerprint, long now, String reason, Scene scene) {
        lastCandidateFingerprint = fingerprint;
        lastCandidateAtMs = now;
        candidateStartedLiked = scene.likeControlLiked();
        scheduleDwellCheck(fingerprint);
        debug("candidate: " + reason + " | waiting " + REEL_DWELL_MS + "ms | fp="
                + shortFingerprint(fingerprint) + " | " + scene.signalSummary(), true);
    }

    private void scheduleDwellCheck(String fingerprint) {
        if (pendingDwellRunnable != null) {
            mainHandler.removeCallbacks(pendingDwellRunnable);
        }

        pendingDwellRunnable = () -> {
            long now = SystemClock.elapsedRealtime();
            if (!wasInReels || !fingerprint.equals(lastCandidateFingerprint)) {
                return;
            }
            if (canCount(now, fingerprint)) {
                recordReel(fingerprint, now, "counted: delayed dwell");
            } else {
                debug("skip: cooldown/duplicate | fp=" + shortFingerprint(fingerprint), true);
            }
        };
        mainHandler.postDelayed(pendingDwellRunnable, REEL_DWELL_MS);
    }

    private void recordReel(String fingerprint, long now, String reason) {
        DoomStatsStore.recordReel(this);
        lastCountedFingerprint = fingerprint;
        lastCountAtMs = now;
        lastSameFingerprintCountAtMs = now;
        lastCandidateFingerprint = fingerprint;
        debug(reason + " | fp=" + shortFingerprint(fingerprint), true);
        FirebaseRepo.scheduleSync(this);
        sendStatsBroadcast();
    }

    private void maybeRecordLike(Scene scene, String fingerprint) {
        if (candidateStartedLiked || !scene.likeControlLiked() || fingerprint.equals(lastLikedFingerprint)) {
            return;
        }

        DoomStatsStore.recordLike(this);
        lastLikedFingerprint = fingerprint;
        candidateStartedLiked = true;
        debug("liked: +1 xp | fp=" + shortFingerprint(fingerprint), true);
        FirebaseRepo.scheduleSync(this);
        sendStatsBroadcast();
    }

    private void debug(String line, boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (!force && now - lastDebugAtMs < DEBUG_THROTTLE_MS) {
            return;
        }
        lastDebugAtMs = now;
        DoomStatsStore.recordDebug(this, line);
        sendStatsBroadcast();
    }

    private void sendStatsBroadcast() {
        sendBroadcast(new Intent(DoomStatsStore.ACTION_STATS_CHANGED).setPackage(getPackageName()));
    }

    private void resetReelsState() {
        wasInReels = false;
        reelsEnteredAtMs = 0L;
        lastCandidateFingerprint = "";
        lastScrollToken = "";
        candidateStartedLiked = false;
        cancelPendingCandidate();
    }

    private void cancelPendingCandidate() {
        if (pendingDwellRunnable != null) {
            mainHandler.removeCallbacks(pendingDwellRunnable);
            pendingDwellRunnable = null;
        }
    }

    private static String shortFingerprint(String fingerprint) {
        int split = fingerprint.indexOf(':');
        return split > 0 ? fingerprint.substring(0, split) : fingerprint;
    }

    private static final class Scene {
        private final String text;
        private final String ids;
        private final String classes;
        private final boolean hasScrollableNode;
        private final boolean likeControlLiked;

        private Scene(String text, String ids, String classes, boolean hasScrollableNode, boolean likeControlLiked) {
            this.text = text;
            this.ids = ids;
            this.classes = classes;
            this.hasScrollableNode = hasScrollableNode;
            this.likeControlLiked = likeControlLiked;
        }

        static Scene from(AccessibilityNodeInfo root) {
            StringBuilder text = new StringBuilder();
            StringBuilder ids = new StringBuilder();
            StringBuilder classes = new StringBuilder();
            boolean[] scrollable = new boolean[]{false};
            boolean[] liked = new boolean[]{false};
            collect(root, text, ids, classes, scrollable, liked, 0, new int[]{0});
            return new Scene(
                    text.toString().toLowerCase(Locale.US),
                    ids.toString().toLowerCase(Locale.US),
                    classes.toString().toLowerCase(Locale.US),
                    scrollable[0],
                    liked[0]
            );
        }

        boolean looksLikeReelsScreen() {
            String all = text + " " + ids + " " + classes;
            if (isOverlayOpen(all)) {
                return false;
            }

            boolean explicitReels = all.contains("reels")
                    || all.contains("clips")
                    || all.contains("clips_viewer")
                    || all.contains("clip_viewer")
                    || all.contains("reel_viewer");

            boolean hasReelControls = containsAny(all, "like", "comment")
                    && containsAny(all, "share", "send")
                    && containsAny(all, "audio", "original audio", "follow");

            return explicitReels || (hasScrollableNode && hasReelControls);
        }

        boolean hasAdSignal() {
            String all = text + " " + ids;
            return all.contains("sponsored")
                    || all.contains("ad choices")
                    || all.contains("about this ad")
                    || all.contains("why you're seeing this ad")
                    || all.contains("why you are seeing this ad");
        }

        boolean likeControlLiked() {
            return likeControlLiked;
        }

        String signalSummary() {
            String all = text + " " + ids + " " + classes;
            return "reels=" + containsAny(all, "reels", "clips", "clip_viewer")
                    + ", controls=" + (containsAny(all, "like", "comment")
                    && containsAny(all, "share", "send"))
                    + ", scrollable=" + hasScrollableNode
                    + ", ad=" + hasAdSignal()
                    + ", liked=" + likeControlLiked;
        }

        String reelFingerprint() {
            String compact = (text + " " + ids)
                    .replaceAll("\\b(like|liked|comment|comments|share|send|more|reels|clips|home|search|profile|follow|following)\\b", " ")
                    .replaceAll("\\b(view translation|original audio|audio)\\b", " ")
                    .replaceAll("\\d+[,.]?\\d*[kKmM]?", " ")
                    .replaceAll("[^a-z0-9_@# ]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (compact.length() > 260) {
                compact = compact.substring(0, 260);
            }
            return Integer.toHexString(compact.hashCode()) + ":" + compact;
        }

        private static boolean isOverlayOpen(String all) {
            return all.contains("add a comment")
                    || all.contains("write a message")
                    || all.contains("send to")
                    || all.contains("search users")
                    || all.contains("report")
                    || all.contains("not interested");
        }

        private static boolean containsAny(String value, String first, String second) {
            return value.contains(first) || value.contains(second);
        }

        private static boolean containsAny(String value, String first, String second, String third) {
            return value.contains(first) || value.contains(second) || value.contains(third);
        }

        private static void collect(
                AccessibilityNodeInfo node,
                StringBuilder text,
                StringBuilder ids,
                StringBuilder classes,
                boolean[] scrollable,
                boolean[] liked,
                int depth,
                int[] parts
        ) {
            if (node == null || depth > 10 || parts[0] >= MAX_NODE_PARTS) {
                return;
            }

            if (node.isScrollable()) {
                scrollable[0] = true;
            }
            if (looksLikeLikedButton(node)) {
                liked[0] = true;
            }

            append(node.getText(), text, parts);
            append(node.getContentDescription(), text, parts);
            append(node.getViewIdResourceName(), ids, parts);
            append(node.getClassName(), classes, parts);

            for (int i = 0; i < node.getChildCount() && parts[0] < MAX_NODE_PARTS; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) {
                    continue;
                }
                try {
                    collect(child, text, ids, classes, scrollable, liked, depth + 1, parts);
                } finally {
                    child.recycle();
                }
            }
        }

        private static boolean looksLikeLikedButton(AccessibilityNodeInfo node) {
            String content = node.getContentDescription() == null ? "" : node.getContentDescription().toString().trim().toLowerCase(Locale.US);
            String id = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName().toLowerCase(Locale.US);

            if (content.equals("liked") || content.equals("unlike") || content.startsWith("unlike ")) {
                return true;
            }
            return id.contains("like") && node.isSelected();
        }

        private static void append(CharSequence value, StringBuilder out, int[] parts) {
            if (value == null) {
                return;
            }
            String item = value.toString().trim();
            if (item.isEmpty()) {
                return;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(item);
            parts[0]++;
        }
    }
}
