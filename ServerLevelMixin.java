package cn.kafei.mixin;

import cn.kafei.SilentMenuState;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a silent open/close is running, drop sounds and container game events (sculk vibrations).
 * This is what makes modded containers quiet. require = 0 so a signature mismatch only logs a warning.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void quietly$muteSoundAt(Player player, double x, double y, double z, Holder<SoundEvent> sound,
                                     SoundSource source, float volume, float pitch, long seed, CallbackInfo ci) {
        if (SilentMenuState.isMuting()) {
            ci.cancel();
        }
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void quietly$muteSoundEntity(Player player, Entity entity, Holder<SoundEvent> sound,
                                         SoundSource source, float volume, float pitch, long seed, CallbackInfo ci) {
        if (SilentMenuState.isMuting()) {
            ci.cancel();
        }
    }

    @Inject(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void quietly$muteGameEvent(GameEvent event, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        if (SilentMenuState.isMuting() && (event == GameEvent.CONTAINER_OPEN || event == GameEvent.CONTAINER_CLOSE)) {
            ci.cancel();
        }
    }
}
