package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * For vanilla chests / barrels / ender chests Quietly skips Container#startOpen / #stopOpen entirely
 * (no opener count, no trapped-chest redstone) and animates the lid itself. Modded containers keep
 * their own startOpen/stopOpen; sounds and game events are muted by ServerLevelMixin instead.
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {

    @Redirect(
            method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;startOpen(Lnet/minecraft/world/entity/player/Player;)V"))
    private void quietly$startOpen(Container container, Player opener) {
        if (SilentMenuState.isSkippingStartOpen()) {
            SilentMenuState.markSkipped((AbstractContainerMenu) (Object) this);
            return;
        }
        container.startOpen(opener);
    }

    @Redirect(
            method = "removed(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;stopOpen(Lnet/minecraft/world/entity/player/Player;)V"))
    private void quietly$stopOpen(Container container, Player player) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (SilentMenuState.isSkipped(menu)) {
            SilentMenuState.clearSkipped(menu);
            if (container instanceof PlayerEnderChestContainer enderInventory) {
                enderInventory.setActiveChest(null);
            }
            return;
        }
        container.stopOpen(player);
    }
}
