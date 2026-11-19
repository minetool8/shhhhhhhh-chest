package cn.kafei;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import cn.kafei.net.QuietlyNetwork;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class SilentOpenManager {
    public static final int BASE_OPEN_TICKS = 24;
    public static final int TICKS_PER_ITEM_KIND = 8;

    /** If true you must keep holding right-click (and crouch) for the whole time; releasing cancels. */
    public static final boolean REQUIRE_HOLDING_CLICK = true;
    /**
     * The client re-sends the use packet roughly every 4-5 ticks while right-click is held, so a click
     * older than this many server ticks means the button was released.
     */
    public static final int HOLD_TIMEOUT_TICKS = 10;

    private static final Map<UUID, ActiveSilentOpen> ACTIVE_OPENS = new HashMap<>();
    private static final Map<AbstractContainerMenu, AnimationTarget> MENU_ANIMATIONS = new WeakHashMap<>();
    private static final Map<AnimationKey, Integer> ANIMATION_REF_COUNTS = new HashMap<>();

    private SilentOpenManager() {}

    // ------------------------------------------------------------------ public API

    public static void startOrRefresh(ServerPlayer player, BlockPos pos, BlockHitResult hit) {
        OpenTarget target = resolveOpenTarget(player, pos);
        if (target == null) {
            cancel(player.getUUID());
            return;
        }
        ActiveSilentOpen existing = ACTIVE_OPENS.get(player.getUUID());
        if (existing != null && existing.matches(player.serverLevel(), pos)) {
            existing.refresh(target.itemKinds(), hit);
            return;
        }
        cancel(player.getUUID());
        ACTIVE_OPENS.put(player.getUUID(), new ActiveSilentOpen(player, pos, target.itemKinds(), hit));
    }

    public static void tickActiveOpens() {
        if (ACTIVE_OPENS.isEmpty()) {
            return;
        }
        for (ActiveSilentOpen open : new ArrayList<>(ACTIVE_OPENS.values())) {
            if (open.tick()) {
                ACTIVE_OPENS.remove(open.player.getUUID(), open);
            }
        }
    }

    public static void cancel(UUID playerId) {
        ActiveSilentOpen open = ACTIVE_OPENS.remove(playerId);
        if (open != null) {
            open.close();
        }
    }

    public static void clearAll() {
        for (ActiveSilentOpen open : new ArrayList<>(ACTIVE_OPENS.values())) {
            open.close();
        }
        ACTIVE_OPENS.clear();
        MENU_ANIMATIONS.clear();
        ANIMATION_REF_COUNTS.clear();
    }

    /** Called from the menu mixins when a silently opened menu is closed. */
    public static void onSilentMenuClosed(AbstractContainerMenu menu) {
        AnimationTarget target = MENU_ANIMATIONS.remove(menu);
        if (target == null) {
            return;
        }
        AnimationKey key = target.key();
        int remaining = ANIMATION_REF_COUNTS.getOrDefault(key, 0) - 1;
        if (remaining <= 0) {
            ANIMATION_REF_COUNTS.remove(key);
            target.apply(false);
        } else {
            ANIMATION_REF_COUNTS.put(key, remaining);
        }
    }

    // ------------------------------------------------------------------ internals

    private static boolean canKeepOpening(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!player.isAlive() || player.isRemoved() || !player.isShiftKeyDown()) {
            return false;
        }
        if (player.serverLevel() != level || player.containerMenu != player.inventoryMenu) {
            return false;
        }
        if (!QuietlyCommon.isSupportedSilentContainer(level, pos)) {
            return false;
        }
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 1.0D;
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= reach * reach;
    }

    @Nullable
    private static OpenTarget resolveOpenTarget(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof EnderChestBlock && QuietlyCommon.isVanillaContainerBlock(block)) {
            BlockEntity be = level.getBlockEntity(pos);
            BlockPos above = pos.above();
            if (!(be instanceof EnderChestBlockEntity enderChest)
                    || level.getBlockState(above).isRedstoneConductor(level, above)) {
                return null;
            }
            PlayerEnderChestContainer inventory = player.getEnderChestInventory();
            MenuProvider provider = createEnderChestProvider(inventory, enderChest);
            return new OpenTarget(provider, countKinds(player, inventory), AnimationTarget.forEnderChest(level, pos), false);
        }

        MenuProvider provider = state.getMenuProvider(level, pos);
        BlockEntity be = level.getBlockEntity(pos);

        if (!QuietlyCommon.isVanillaContainerBlock(block)) {
            // Modded / other containers: run the block's own use() later, with sounds muted.
            if (be == null || !QuietlyCommon.isSupportedSilentContainer(level, pos)) {
                return null;
            }
            return new OpenTarget(provider, countStoredKinds(player, be), null, true);
        }

        if (provider == null) {
            return null;
        }

        if (block instanceof ChestBlock chestBlock) {
            Container container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
            return new OpenTarget(provider, countKinds(player, container), AnimationTarget.forChest(level, pos, state), false);
        }

        int kinds = be instanceof Container c ? countKinds(player, c) : 0;
        if (block instanceof BarrelBlock) {
            return new OpenTarget(provider, kinds, AnimationTarget.forBarrel(level, pos), false);
        }
        if (block instanceof ShulkerBoxBlock) {
            return new OpenTarget(provider, kinds, AnimationTarget.forShulkerBox(level, pos), false);
        }
        return null;
    }

    private static int countStoredKinds(ServerPlayer player, BlockEntity be) {
        if (be instanceof Container container) {
            return countKinds(player, container);
        }
        int[] result = new int[1];
        be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            Set<Item> kinds = new HashSet<>();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    kinds.add(stack.getItem());
                }
            }
            result[0] = kinds.size();
        });
        return result[0];
    }

    private static MenuProvider createEnderChestProvider(PlayerEnderChestContainer inventory, EnderChestBlockEntity chest) {
        inventory.setActiveChest(chest);
        return new SimpleMenuProvider(
                (id, playerInventory, p) -> ChestMenu.threeRows(id, playerInventory, inventory),
                Component.translatable("container.enderchest"));
    }

    private static int countKinds(ServerPlayer player, @Nullable Container container) {
        if (container == null) {
            return 0;
        }
        Set<Item> kinds = new HashSet<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && RummageCompat.isSlotVisible(player, container, slot)) {
                kinds.add(stack.getItem());
            }
        }
        return kinds.size();
    }

    private static void openSilently(ServerPlayer player, BlockPos pos, OpenTarget target, @Nullable BlockHitResult hit) {
        // Vanilla containers: skip startOpen/stopOpen and animate ourselves.
        // Everything else: let the block do its normal use(), with sounds/events muted.
        SilentMenuState.beginSilentOpen(!target.viaUse());
        try {
            if (target.viaUse()) {
                ServerLevel level = player.serverLevel();
                BlockHitResult useHit = hit != null ? hit
                        : new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND, useHit);
            } else {
                player.openMenu(target.menuProvider());
            }
            AbstractContainerMenu menu = player.containerMenu;
            if (menu != player.inventoryMenu) {
                SilentMenuState.markSilentMenu(menu);
                registerOpenAnimation(menu, target.animationTarget());
            }
        } finally {
            SilentMenuState.endSilentOpen();
        }
    }

    private static void registerOpenAnimation(AbstractContainerMenu menu, @Nullable AnimationTarget target) {
        if (target == null || !SilentMenuState.isSilentMenu(menu)) {
            return;
        }
        MENU_ANIMATIONS.put(menu, target);
        AnimationKey key = target.key();
        int count = ANIMATION_REF_COUNTS.getOrDefault(key, 0) + 1;
        ANIMATION_REF_COUNTS.put(key, count);
        if (count == 1) {
            target.apply(true);
        }
    }

    // ------------------------------------------------------------------ types

    private record OpenTarget(@Nullable MenuProvider menuProvider, int itemKinds, @Nullable AnimationTarget animationTarget, boolean viaUse) {}

    private enum AnimationType { CHEST, ENDER_CHEST, BARREL, SHULKER_BOX }

    private record AnimationKey(ServerLevel level, BlockPos pos, AnimationType type) {}

    private record AnimationTarget(ServerLevel level, BlockPos pos, @Nullable BlockPos other, AnimationType type) {
        static AnimationTarget forChest(ServerLevel level, BlockPos pos, BlockState state) {
            BlockPos first = pos;
            BlockPos second = null;
            if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                second = pos.relative(ChestBlock.getConnectedDirection(state));
                if (second.asLong() < first.asLong()) {
                    BlockPos swap = first;
                    first = second;
                    second = swap;
                }
            }
            return new AnimationTarget(level, first.immutable(), second == null ? null : second.immutable(), AnimationType.CHEST);
        }

        static AnimationTarget forBarrel(ServerLevel level, BlockPos pos) {
            return new AnimationTarget(level, pos.immutable(), null, AnimationType.BARREL);
        }

        static AnimationTarget forEnderChest(ServerLevel level, BlockPos pos) {
            return new AnimationTarget(level, pos.immutable(), null, AnimationType.ENDER_CHEST);
        }

        static AnimationTarget forShulkerBox(ServerLevel level, BlockPos pos) {
            return new AnimationTarget(level, pos.immutable(), null, AnimationType.SHULKER_BOX);
        }

        AnimationKey key() {
            return new AnimationKey(level, pos, type);
        }

        void apply(boolean open) {
            if (type == AnimationType.BARREL) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BarrelBlock && state.hasProperty(BlockStateProperties.OPEN)) {
                    // Plain block-state change: no sound, no CONTAINER_OPEN game event.
                    level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 3);
                }
                return;
            }
            sendLidEvent(pos, open);
            if (other != null) {
                sendLidEvent(other, open);
            }
        }

        private void sendLidEvent(BlockPos target, boolean open) {
            BlockState state = level.getBlockState(target);
            Block block = state.getBlock();
            boolean valid = switch (type) {
                case CHEST -> block instanceof ChestBlock;
                case ENDER_CHEST -> block instanceof EnderChestBlock;
                case SHULKER_BOX -> block instanceof ShulkerBoxBlock;
                default -> false;
            };
            if (valid) {
                // Block event 1 = lid/shulker animation; this is what vanilla sends, minus the sound.
                level.blockEvent(target, block, 1, open ? 1 : 0);
            }
        }
    }

    private static final class ActiveSilentOpen {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final BlockPos pos;
        private int totalTicks;
        private int elapsedTicks;
        private int lastClickTick;
        @Nullable
        private BlockHitResult hit;

        ActiveSilentOpen(ServerPlayer player, BlockPos pos, int itemKinds, BlockHitResult hit) {
            this.hit = hit;
            this.lastClickTick = player.server.getTickCount();
            this.player = player;
            this.level = player.serverLevel();
            this.pos = pos.immutable();
            this.totalTicks = ticksFor(itemKinds);
            QuietlyNetwork.sendProgress(player, 0.0F);
        }

        private static int ticksFor(int itemKinds) {
            return BASE_OPEN_TICKS + itemKinds * TICKS_PER_ITEM_KIND;
        }

        boolean matches(ServerLevel otherLevel, BlockPos otherPos) {
            return this.level == otherLevel && this.pos.equals(otherPos);
        }

        void refresh(int itemKinds, BlockHitResult newHit) {
            this.totalTicks = ticksFor(itemKinds);
            this.hit = newHit;
            this.lastClickTick = player.server.getTickCount();
        }

        /** @return true when this open is finished (opened or cancelled) and should be removed. */
        boolean tick() {
            if (!canKeepOpening(player, level, pos)) {
                close();
                return true;
            }
            if (REQUIRE_HOLDING_CLICK && player.server.getTickCount() - lastClickTick > HOLD_TIMEOUT_TICKS) {
                close(); // right-click released
                return true;
            }
            elapsedTicks++;
            QuietlyNetwork.sendProgress(player, Math.min(1.0F, (float) elapsedTicks / (float) Math.max(1, totalTicks)));
            if (elapsedTicks >= totalTicks) {
                OpenTarget target = resolveOpenTarget(player, pos);
                close();
                if (target != null) {
                    openSilently(player, pos, target, hit);
                }
                return true;
            }
            return false;
        }

        void close() {
            QuietlyNetwork.sendProgress(player, -1.0F);
        }
    }
}
