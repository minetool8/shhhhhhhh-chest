package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin {

    @Redirect(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V",
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
            return;
        }
        container.stopOpen(player);
    }
}
