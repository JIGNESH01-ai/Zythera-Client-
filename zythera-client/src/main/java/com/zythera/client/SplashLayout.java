package com.zythera.client;

/** Responsive framebuffer-space geometry, independent of Minecraft GUI scale. */
public record SplashLayout(int width, int height, float unit, int logoTop, int logoHeight,
                           int statusY, int barY, int barWidth, int barHeight,
                           int percentY, int resourceY, int tipY) {
    public static SplashLayout forSize(int width, int height) {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Invalid framebuffer size");
        float unit = Math.min(width / 480f, height / 270f);
        int logoTop = Math.round(height * .09f);
        int logoHeight = Math.max(1, Math.round(height * .45f));
        int barWidth = Math.max(1, Math.min(Math.round(width * .66f), Math.round(350 * unit)));
        return new SplashLayout(width, height, unit, logoTop, logoHeight,
            Math.round(height * .57f), Math.round(height * .64f), barWidth,
            Math.max(2, Math.round(6 * unit)), Math.round(height * .69f),
            Math.round(height * .76f), Math.round(height * .85f));
    }
}
