package cn.kafei;

import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class SilentMenuState {
    private static final ThreadLocal<Boolean> OPENING_SILENTLY = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> SKIP_START_OPEN = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> CLOSING_SILENTLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Every menu Quietly opened (used to mute the close and release the lid animation). */
    private static final Set<AbstractContainerMenu> SILENT_MENUS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    /** Menus whose Container#startOpen we skipped, so we must also skip stopOpen. */
    private static final Set<AbstractContainerMenu> SKIPPED_MENUS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private SilentMenuState() {}

    public static void beginSilentOpen(boolean skipStartOpen) {
        OPENING_SILENTLY.set(Boolean.TRUE);
        SKIP_START_OPEN.set(skipStartOpen);
    }

    public static void endSilentOpen() {
        OPENING_SILENTLY.remove();
        SKIP_START_OPEN.remove();
    }

    public static boolean isOpeningSilently() { return OPENING_SILENTLY.get(); }
    public static boolean isSkippingStartOpen() { return OPENING_SILENTLY.get() && SKIP_START_OPEN.get(); }

    public static void beginCloseMute() { CLOSING_SILENTLY.set(Boolean.TRUE); }
    public static void endCloseMute() { CLOSING_SILENTLY.remove(); }

    /** True while a silent open or close is running: container sounds / game events are dropped. */
    public static boolean isMuting() { return OPENING_SILENTLY.get() || CLOSING_SILENTLY.get(); }

    public static void markSilentMenu(AbstractContainerMenu menu) { SILENT_MENUS.add(menu); }
    public static boolean isSilentMenu(AbstractContainerMenu menu) { return SILENT_MENUS.contains(menu); }
    public static void clearSilentMenu(AbstractContainerMenu menu) { SILENT_MENUS.remove(menu); }

    public static void markSkipped(AbstractContainerMenu menu) { SKIPPED_MENUS.add(menu); }
    public static boolean isSkipped(AbstractContainerMenu menu) { return SKIPPED_MENUS.contains(menu); }
    public static void clearSkipped(AbstractContainerMenu menu) { SKIPPED_MENUS.remove(menu); }
}
