package com.zythera.client;

/** Aspect-preserving texture geometry in framebuffer pixels. */
public final class TextureLayout {
    private TextureLayout() {}
    public record Placement(double x, double y, double scale) {}
    public static Placement cover(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = Math.max(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
        return new Placement((targetWidth - sourceWidth * scale) / 2, (targetHeight - sourceHeight * scale) / 2, scale);
    }
    public static Placement contain(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = Math.min(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
        return new Placement((targetWidth - sourceWidth * scale) / 2, (targetHeight - sourceHeight * scale) / 2, scale);
    }
}
