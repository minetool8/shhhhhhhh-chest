package cn.kafei;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Optional soft integration with the Rummage mod (reflection only, no compile-time dependency).
 * Rummage hides the items of an un-searched container, so the silent-open timer must not
 * count item kinds the player has not rummaged yet (it would leak what is inside).
 * Any failure disables the integration and falls back to counting everything.
 */
public final class RummageCompat {
    private static boolean initialized;
    private static boolean available;
    private static Method getTarget;      // CommonContainerUtil.getTarget(Container, int)
    private static Method targetEntity;   // RummageTarget.entity()
    private static Method targetSlot;     // RummageTarget.localSlotIndex()
    private static Method needRummage;    // IRummageable.isNeedRummage(Player)
    private static Method slotRummaged;   // IRummageable.isSlotRummaged(Player, int)

    private RummageCompat() {}

    private static synchronized boolean ensureInit() {
        if (!initialized) {
            initialized = true;
            try {
                if (ModList.get().isLoaded("rummage")) {
                    Class<?> util = Class.forName("com.scarasol.rummage.util.CommonContainerUtil");
                    Class<?> target = Class.forName("com.scarasol.rummage.data.RummageTarget");
                    Class<?> rummageable = Class.forName("com.scarasol.rummage.api.mixin.IRummageable");
                    getTarget = util.getMethod("getTarget", Container.class, int.class);
                    targetEntity = target.getMethod("entity");
                    targetSlot = target.getMethod("localSlotIndex");
                    needRummage = rummageable.getMethod("isNeedRummage", Player.class);
                    slotRummaged = rummageable.getMethod("isSlotRummaged", Player.class, int.class);
                    available = true;
                    QuietlyMod.LOGGER.info("Rummage detected: silent-open timing only counts rummaged slots");
                }
            } catch (Throwable t) {
                available = false;
                QuietlyMod.LOGGER.warn("Rummage compat disabled: {}", t.toString());
            }
        }
        return available;
    }

    /** @return false only when Rummage is hiding this slot's item from the player. */
    public static boolean isSlotVisible(ServerPlayer player, Container container, int slot) {
        if (!ensureInit()) {
            return true;
        }
        try {
            Object target = getTarget.invoke(null, container, slot);
            if (target == null) {
                return true;
            }
            Object entity = targetEntity.invoke(target);
            if (entity == null || !(Boolean) needRummage.invoke(entity, player)) {
                return true;
            }
            int local = (Integer) targetSlot.invoke(target);
            return (Boolean) slotRummaged.invoke(entity, player, local);
        } catch (Throwable t) {
            available = false;
            QuietlyMod.LOGGER.warn("Rummage compat disabled after error: {}", t.toString());
            return true;
        }
    }
}
