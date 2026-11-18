package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import cn.kafei.SilentOpenManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mutes the close of a silently opened menu (any container, modded included) and releases its lid animation. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "doCloseContainer()V", at = @At("HEAD"))
    private void quietly$closeHead(CallbackInfo ci) {
        AbstractContainerMenu menu = ((ServerPlayer) (Object) this).containerMenu;
        if (SilentMenuState.isSilentMenu(menu)) {
            SilentMenuState.beginCloseMute();
            SilentOpenManager.onSilentMenuClosed(menu);
            SilentMenuState.clearSilentMenu(menu);
        }
    }

    @Inject(method = "doCloseContainer()V", at = @At("RETURN"))
    private void quietly$closeReturn(CallbackInfo ci) {
        SilentMenuState.endCloseMute();
    }
}
