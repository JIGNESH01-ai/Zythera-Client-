package com.zythera.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextureLayoutTest {
    @Test void preservesAspectForWideAndTallScreens() {
        for (int[] size : new int[][] {{1920, 1080}, {1080, 1920}, {1280, 720}, {320, 240}}) {
            var p = TextureLayout.contain(1195, 485, size[0], size[1]);
            assertTrue(1195 * p.scale() <= size[0] + 0.001);
            assertTrue(485 * p.scale() <= size[1] + 0.001);
            assertEquals(1195.0 / 485, (1195 * p.scale()) / (485 * p.scale()), 1e-9);
            var bg = TextureLayout.cover(1000, 775, size[0], size[1]);
            assertTrue(1000 * bg.scale() >= size[0] - 0.001);
            assertTrue(775 * bg.scale() >= size[1] - 0.001);
        }
    }
}
