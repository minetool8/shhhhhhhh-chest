package cn.kafei.net;

/** Latest silent-open progress received from the server (client side). Plain Java, safe on any dist. */
public final class ClientProgress {
    private static volatile float progress = -1.0F;
    private static volatile long lastUpdateMs = 0L;

    private ClientProgress() {}

    public static void set(float value) {
        progress = value;
        lastUpdateMs = System.currentTimeMillis();
    }

    /** @return 0..1 while active, or -1 when nothing is being opened. */
    public static float get() {
        if (progress < 0.0F || System.currentTimeMillis() - lastUpdateMs > 500L) {
            return -1.0F;
        }
        return progress;
    }
}
