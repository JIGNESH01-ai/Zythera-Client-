package com.zythera.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SplashLayoutTest {
    @Test void contentRemainsOrderedAndInsideDifferentFramebuffers() {
        for (int[] size : new int[][] {{1920,1080}, {854,480}, {480,854}, {320,180}, {2400,1080}}) {
            SplashLayout l = SplashLayout.forSize(size[0], size[1]);
            assertTrue(l.logoTop() > 0);
            assertTrue(l.logoTop() + l.logoHeight() < l.statusY());
            assertTrue(l.statusY() < l.barY());
            assertTrue(l.barY() + l.barHeight() < l.percentY());
            assertTrue(l.percentY() < l.resourceY());
            assertTrue(l.resourceY() < l.tipY());
            assertTrue(l.tipY() + 12*l.unit() < l.height());
            assertTrue(l.barWidth() < l.width());
        }
    }
    @Test void rejectsInvalidFramebuffers() {
        assertThrows(IllegalArgumentException.class, () -> SplashLayout.forSize(0, 100));
    }
}
