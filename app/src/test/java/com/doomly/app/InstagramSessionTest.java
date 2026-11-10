package com.doomly.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class InstagramSessionTest {
    @Test public void shortSessionsNeverQualifyAndCannotAccumulate() {
        InstagramSession session = new InstagramSession();
        for (int i = 0; i < 3; i++) {
            session.observeForeground(i * (InstagramSession.LONG_SESSION_MS + 60_000L));
            session.recordReel();
            observeFor(session, i * (InstagramSession.LONG_SESSION_MS + 60_000L), InstagramSession.LONG_SESSION_MS - 5_000L);
            assertEquals(0, session.finish());
        }
    }
    @Test public void onlyLongForegroundSessionsWithReelsQualifyOnce() {
        InstagramSession session = new InstagramSession();
        session.observeForeground(100);
        session.recordReel();
        observeFor(session, 100, InstagramSession.LONG_SESSION_MS);
        assertEquals(InstagramSession.LONG_SESSION_MS, session.finish());
        assertEquals(0, session.finish());
        session.observeForeground(200);
        observeFor(session, 200, InstagramSession.LONG_SESSION_MS);
        assertEquals(0, session.finish());
    }
    @Test public void screenOffOrInterruptionCancelsRatherThanCompletes() {
        InstagramSession session = new InstagramSession();
        session.observeForeground(100);
        session.recordReel();
        observeFor(session, 100, InstagramSession.LONG_SESSION_MS);
        assertTrue(session.stale(100 + InstagramSession.LONG_SESSION_MS + 16_000L));
        session.cancel();
        assertEquals(0, session.finish());
    }
    @Test public void reelMilestonesResetBetweenSessions() {
        InstagramSession session = new InstagramSession();
        session.recordReel();
        assertEquals(0, session.reelCount());
        session.observeForeground(0);
        for (int i = 0; i < 10; i++) session.recordReel();
        assertEquals(10, session.reelCount());
        session.observeForeground(20_000);
        assertEquals(0, session.reelCount());
        session.recordReel();
        session.finish();
        assertEquals(0, session.reelCount());
    }
    private void observeFor(InstagramSession session, long start, long duration) {
        for (long elapsed = 5_000L; elapsed <= duration; elapsed += 5_000L) session.observeForeground(start + elapsed);
    }
}
