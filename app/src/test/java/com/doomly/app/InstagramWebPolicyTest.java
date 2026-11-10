package com.doomly.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class InstagramWebPolicyTest {
    @Test public void sessionCookieMeansAuthenticated() {
        assertTrue(InstagramWebPolicy.isAuthenticatedCookie("csrftoken=abc; sessionid=xyz%3A1; mid=1"));
        assertTrue(InstagramWebPolicy.isAuthenticatedCookie("ds_user_id=12345"));
    }
    @Test public void anonymousCookiesAreNotAuthenticated() {
        assertFalse(InstagramWebPolicy.isAuthenticatedCookie("csrftoken=abc; ig_did=1; mid=2"));
        assertFalse(InstagramWebPolicy.isAuthenticatedCookie("sessionid=; ds_user_id="));
        assertFalse(InstagramWebPolicy.isAuthenticatedCookie(null));
        assertFalse(InstagramWebPolicy.isAuthenticatedCookie(""));
    }
    @Test public void httpErrorsSurfaceOurMessageNotChromiumPage() {
        assertNull(InstagramWebPolicy.errorMessage(200));
        assertNull(InstagramWebPolicy.errorMessage(302));
        assertEquals("Too many requests. Try again later.", InstagramWebPolicy.errorMessage(429));
        assertEquals("Couldn't load. Check connection.", InstagramWebPolicy.errorMessage(-1));
        assertEquals("Instagram error 403", InstagramWebPolicy.errorMessage(403));
        assertTrue(InstagramWebPolicy.isErrorStatus(500));
        assertFalse(InstagramWebPolicy.isErrorStatus(200));
    }
    @Test public void hostsAndUrls() {
        assertTrue(InstagramWebPolicy.isInstagramHost("www.instagram.com"));
        assertTrue(InstagramWebPolicy.isInstagramHost("scontent.cdninstagram.com".replace("cdninstagram.com","fbcdn.net")));
        assertFalse(InstagramWebPolicy.isInstagramHost("evil-instagram.com"));
        assertFalse(InstagramWebPolicy.isInstagramHost(null));
        assertTrue(InstagramWebPolicy.isLoginUrl("https://www.instagram.com/accounts/login/"));
        assertTrue(InstagramWebPolicy.isReelsUrl("https://www.instagram.com/reels/"));
        assertTrue(InstagramWebPolicy.isExternalScheme("intent"));
        assertFalse(InstagramWebPolicy.isExternalScheme("https"));
    }
}
