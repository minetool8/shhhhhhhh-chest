package cn.kafei;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod(QuietlyMod.MOD_ID)
public class QuietlyMod {
    public static final String MOD_ID = "quietly";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuietlyMod() {
        // Server-side mod: clients without it may still join.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Quietly (Forge 1.20.1 port) initialized");
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!QuietlyCommon.shouldStartSilentOpen(player, event.getLevel(), event.getPos())) {
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            SilentOpenManager.startOrRefresh(player, event.getPos());
        }
        // Cancel for both hands so vanilla never opens the container loudly.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SilentOpenManager.tickActiveOpens();
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SilentOpenManager.cancel(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        SilentOpenManager.clearAll();
    }
}
