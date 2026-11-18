package cn.kafei.client;

import cn.kafei.net.ClientProgress;
import net.minecraft.client.gui.GuiGraphics;

/** Square progress ring drawn where the crosshair is. Fills clockwise from the top-left corner. */
public final class SilentProgressOverlay {
    private static final int SIZE = 24;        // outer square size in GUI pixels
    private static final int THICK = 3;        // ring thickness
    private static final int TRACK = 0xB0E6E6E6;
    private static final int FILL = 0xFFF5661E;

    private SilentProgressOverlay() {}

    public static void render(GuiGraphics g, int screenW, int screenH) {
        float progress = ClientProgress.get();
        if (progress < 0.0F) {
            return;
        }
        int x = screenW / 2 - SIZE / 2;
        int y = screenH / 2 - SIZE / 2;
        int s = SIZE;
        int t = THICK;

        // Track (full ring)
        g.fill(x, y, x + s, y + t, TRACK);                       // top
        g.fill(x + s - t, y + t, x + s, y + s, TRACK);           // right
        g.fill(x, y + s - t, x + s - t, y + s, TRACK);           // bottom
        g.fill(x, y + t, x + t, y + s - t, TRACK);               // left

        // Fill, clockwise: top -> right -> bottom -> left
        int topLen = s, rightLen = s - t, bottomLen = s - t, leftLen = s - 2 * t;
        int total = topLen + rightLen + bottomLen + leftLen;
        int remaining = Math.round(Math.min(1.0F, progress) * total);

        int w = Math.min(remaining, topLen);
        if (w > 0) g.fill(x, y, x + w, y + t, FILL);
        remaining -= w;

        int h = Math.min(Math.max(remaining, 0), rightLen);
        if (h > 0) g.fill(x + s - t, y + t, x + s, y + t + h, FILL);
        remaining -= h;

        w = Math.min(Math.max(remaining, 0), bottomLen);
        if (w > 0) g.fill(x + s - t - w, y + s - t, x + s - t, y + s, FILL);
        remaining -= w;

        h = Math.min(Math.max(remaining, 0), leftLen);
        if (h > 0) g.fill(x, y + s - t - h, x + t, y + s - t, FILL);
    }
}
