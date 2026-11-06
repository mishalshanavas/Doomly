package com.doomly.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScrollLevelTest {
    @Test public void thresholdsAndProgressAreConsistent() {
        assertEquals(0, ScrollLevel.forCount(-1).index);
        assertEquals(25, ScrollLevel.forCount(0).next);
        assertEquals(0, ScrollLevel.forCount(24).index);
        assertEquals(1, ScrollLevel.forCount(25).index);
        assertEquals(0f, ScrollLevel.forCount(25).progress, 0f);
        assertEquals(.5f, ScrollLevel.forCount(50).progress, 0f);
        assertEquals(2, ScrollLevel.forCount(75).index);
        assertEquals(3, ScrollLevel.forCount(150).index);
        assertEquals(4, ScrollLevel.forCount(300).index);
        assertEquals(1f, ScrollLevel.forCount(Integer.MAX_VALUE).progress, 0f);
        assertEquals(0, ScrollLevel.forCount(Integer.MAX_VALUE).next);
    }
}
