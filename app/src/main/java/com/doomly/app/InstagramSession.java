package com.doomly.app;

/** Conservative foreground duration: background time and screen-off time never count. */
public final class InstagramSession {
    public static final long LONG_SESSION_MS = 10 * 60_000L;
    private long started = -1, lastSeen = -1;
    private boolean watchedReels;
    public boolean active() { return started >= 0; }
    public boolean stale(long now) { return active() && (now < lastSeen || now - lastSeen > 15_000L); }
    public void observeForeground(long now) {
        // A long period without a confirmed Instagram window is not foreground time.
        if (stale(now)) cancel();
        if (!active()) started = now;
        lastSeen = now;
    }
    public void recordReel() { if (active()) watchedReels = true; }
    public long finish() {
        long duration = active() ? Math.max(0, lastSeen - started) : 0;
        boolean qualifies = watchedReels && duration >= LONG_SESSION_MS;
        cancel();
        return qualifies ? duration : 0;
    }
    public void cancel() { started = -1; lastSeen = -1; watchedReels = false; }
}
