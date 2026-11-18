package cn.kafei.net;

/**
 * Latest silent-open progress from the server (client side), interpolated between the 20 Hz
 * server updates so the bar moves smoothly at any frame rate. Plain Java, safe on any dist.
 */
public final class ClientProgress {
    private static final long TICK_NANOS = 50_000_000L;
    private static final long STALE_NANOS = 500_000_000L;

    private static float from = -1.0F;
    private static float to = -1.0F;
    private static long updateNanos = 0L;

    private ClientProgress() {}

    public static synchronized void set(float value) {
        long now = System.nanoTime();
        if (value < 0.0F) {
            from = -1.0F;
            to = -1.0F;
        } else if (to < 0.0F || now - updateNanos > STALE_NANOS) {
            from = value;
            to = value;
        } else {
            from = currentValue(now); // continue from what is on screen right now
            to = value;
        }
        updateNanos = now;
    }

    /** @return 0..1 while active, or -1 when nothing is being opened. */
    public static synchronized float get() {
        long now = System.nanoTime();
        if (to < 0.0F || now - updateNanos > STALE_NANOS) {
            return -1.0F;
        }
        return currentValue(now);
    }

    private static float currentValue(long now) {
        float t = Math.min(1.0F, Math.max(0.0F, (now - updateNanos) / (float) TICK_NANOS));
        return from + (to - from) * t;
    }
}
