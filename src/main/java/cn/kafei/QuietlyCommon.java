package cn.kafei;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
    /** Add blocks to the tag quietly:ignored (datapack) to exclude them from silent opening. */
    public static final TagKey<Block> IGNORED = TagKey.create(Registries.BLOCK, new ResourceLocation("quietly", "ignored"));

    /**
     * If true, silent opening only starts when both hands are empty. Default is false so held items
     * (TACZ guns in either hand, tools, blocks...) never stop you from opening quietly.
     * Note: with false, sneak + right-click on a container no longer places a held block against it.
     */
    public static final boolean REQUIRE_EMPTY_HANDS = false;

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
        // Anything else (modded included).
        if (state.is(IGNORED)) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false; // crafting tables, anvils etc. stay untouched
        }
        if (be instanceof MenuProvider || state.getMenuProvider(level, pos) != null) {
            return true;
        }
        // Modded storage that opens its GUI from Block#use() without being a MenuProvider
        // (e.g. via NetworkHooks.openScreen): treat inventories as containers.
        if (!be.getClass().getName().startsWith("net.minecraft.")) {
            return be instanceof Container || be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        }
        return false;
    }
}
