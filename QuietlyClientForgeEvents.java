package cn.kafei.client;

import cn.kafei.QuietlyMod;
import cn.kafei.net.ClientProgress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuietlyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class QuietlyClientForgeEvents {
    private QuietlyClientForgeEvents() {}

    /** Hide the vanilla crosshair while the square is shown. */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && ClientProgress.get() >= 0.0F) {
            event.setCanceled(true);
        }
    }
}
