package cn.kafei;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /** True for the vanilla block classes Quietly animates itself (chest, trapped chest, barrel, ender chest, shulker box). */
    public static boolean isVanillaContainerBlock(Block block) {
        return block.getClass().getName().startsWith("net.minecraft.world.level.block.")
                && (block instanceof ChestBlock || block instanceof BarrelBlock
                || block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock);
    }

    public static boolean isSupportedSilentContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof EnderChestBlock && isVanillaContainerBlock(block)) {
            BlockPos above = pos.above();
            return !level.getBlockState(above).isRedstoneConductor(level, above)
                    && level.getBlockEntity(pos) instanceof EnderChestBlockEntity;
        }
        if (isVanillaContainerBlock(block)) {
            return state.getMenuProvider(level, pos) != null;
        }
        // Anything else (modded included): a block entity that opens a menu.
        // Requiring a block entity keeps crafting tables, anvils etc. untouched.
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && (be instanceof MenuProvider || state.getMenuProvider(level, pos) != null);
    }
}
