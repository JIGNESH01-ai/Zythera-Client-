package com.zythera.client;

/** Wall-clock visual sequence, independent of Minecraft's real resource-reload progress. */
public final class LoadingTimeline {
    public static final long MOVE_MS = 1000L;
    public static final long PAUSE_MS = 5000L;
    public static final long COMPLETE_MS = 800L;
    private static final long SEQUENCE_MS = MOVE_MS * 4 + PAUSE_MS * 3;
    private final long startedAt;
    private long completedAt = -1L;

    public LoadingTimeline(long startedAt) { this.startedAt = startedAt; }

    /** Call with a monotonic millisecond clock and the real reload-success state. */
    public double update(long now, boolean resourcesReady) {
        long elapsed = Math.max(0L, now - startedAt);
        if (elapsed < MOVE_MS) return interpolate(1, 50, elapsed);
        elapsed -= MOVE_MS;
        if (elapsed < PAUSE_MS) return 50;
        elapsed -= PAUSE_MS;
        if (elapsed < MOVE_MS) return interpolate(50, 70, elapsed);
        elapsed -= MOVE_MS;
        if (elapsed < PAUSE_MS) return 70;
        elapsed -= PAUSE_MS;
        if (elapsed < MOVE_MS) return interpolate(70, 95, elapsed);
        elapsed -= MOVE_MS;
        if (elapsed < PAUSE_MS) return 95;
        elapsed -= PAUSE_MS;
        if (elapsed < MOVE_MS) return interpolate(95, resourcesReady ? 100 : 99, elapsed);
        if (!resourcesReady) return 99;
        if (completedAt < 0) completedAt = now;
        return 100;
    }
    private static double interpolate(double from, double to, long elapsed) {
        return from + (to - from) * Math.min(1.0, Math.max(0.0, elapsed / (double) MOVE_MS));
    }
    public boolean canClose(long now) { return completedAt >= 0 && now - completedAt >= COMPLETE_MS; }
    public static long sequenceDuration() { return SEQUENCE_MS; }
}
