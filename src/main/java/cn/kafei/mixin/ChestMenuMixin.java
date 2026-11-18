package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import cn.kafei.SilentOpenManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Chests, trapped chests, barrels and ender chests all use ChestMenu. When the menu is being
 * opened by Quietly we skip Container#startOpen / #stopOpen, so vanilla never plays the open/close
 * sound, never emits the CONTAINER_OPEN/CLOSE game event (sculk) and never bumps the opener count.
 * The lid/barrel animation is driven separately by SilentOpenManager.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {

    @Redirect(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;startOpen(Lnet/minecraft/world/entity/player/Player;)V"))
    private void quietly$startOpen(Container container, Player opener) {
        if (SilentMenuState.isOpeningSilently()) {
            SilentMenuState.markSilentMenu((AbstractContainerMenu) (Object) this);
            return;
        }
        container.startOpen(opener);
    }

    @Redirect(
            method = "removed(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;stopOpen(Lnet/minecraft/world/entity/player/Player;)V"))
    private void quietly$stopOpen(Container container, Player player) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (SilentMenuState.isSilentMenu(menu)) {
            SilentOpenManager.onSilentMenuClosed(menu);
            SilentMenuState.clearSilentMenu(menu);
            if (container instanceof PlayerEnderChestContainer enderInventory) {
                enderInventory.setActiveChest(null);
            }
            return;
        }
        container.stopOpen(player);
    }
}
