package cn.kafei;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class QuietlyCommon {
    /**
     * If true, silent opening only starts when both hands are empty, so sneak-placing
     * blocks against a chest still works. Set to false to trigger on any sneak + right-click.
     */
    public static final boolean REQUIRE_EMPTY_HANDS = true;

    private QuietlyCommon() {}

    public static boolean shouldStartSilentOpen(ServerPlayer player, Level level, BlockPos pos) {
        if (!player.isShiftKeyDown() || player.isSpectator()) {
            return false;
        }
        if (REQUIRE_EMPTY_HANDS && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())) {
            return false;
        }
        return isSupportedSilentContainer(level, pos);
    }

    public static boolean isSupportedSilentContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof EnderChestBlock) {
            BlockPos above = pos.above();
            return !level.getBlockState(above).isRedstoneConductor(level, above)
                    && level.getBlockEntity(pos) instanceof EnderChestBlockEntity;
        }
        return state.getMenuProvider(level, pos) != null;
    }
}
