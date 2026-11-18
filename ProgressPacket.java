package cn.kafei.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: current progress 0..1, or a negative value to hide the indicator. */
public record ProgressPacket(float progress) {
    public static void encode(ProgressPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.progress);
    }

    public static ProgressPacket decode(FriendlyByteBuf buf) {
        return new ProgressPacket(buf.readFloat());
    }

    public static void handle(ProgressPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> ClientProgress.set(packet.progress));
        context.setPacketHandled(true);
    }
}
