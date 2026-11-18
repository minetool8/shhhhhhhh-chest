package cn.kafei.net;

import cn.kafei.QuietlyMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class QuietlyNetwork {
    private static final String PROTOCOL = "1";

    // Accept any remote version/absence so vanilla clients can still join.
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(QuietlyMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(v -> true)
            .serverAcceptedVersions(v -> true)
            .simpleChannel();

    private QuietlyNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, ProgressPacket.class,
                ProgressPacket::encode, ProgressPacket::decode, ProgressPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendProgress(ServerPlayer player, float progress) {
        try {
            if (player.connection == null || !CHANNEL.isRemotePresent(player.connection.connection)) {
                return;
            }
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ProgressPacket(progress));
        } catch (Exception ignored) {
            // Player disconnecting; nothing to show.
        }
    }
}
