package com.logistics.core.lib.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The rule for how long a block-backed menu stays open: the block entity it edits must still be
 * standing where the menu was opened, and the player must still be close enough to reach it.
 *
 * <p>Shared by every domain so the mod cannot disagree with itself about reach — a menu that
 * outlives what a packet handler accepts, or vice versa, is how an edit ends up applied to a
 * block the player can no longer see.
 */
public final class MenuValidity {

    /**
     * Interaction reach in blocks, measured from the block centre.
     *
     * <p>Vanilla's own container check is {@code isWithinBlockInteractionRange(pos, 4.0)} — the
     * {@code block_interaction_range} attribute, 4.5 by default, padded by 4 and measured to the
     * block's AABB. Eight blocks from the centre is the equivalent here.
     */
    public static final double MAX_REACH = 8.0;

    private static final double MAX_REACH_SQR = MAX_REACH * MAX_REACH;

    private MenuValidity() {}

    /** True while {@code distanceSqr} from a block centre is inside {@link #MAX_REACH}. */
    public static boolean isWithinReach(double distanceSqr) {
        return distanceSqr <= MAX_REACH_SQR;
    }

    /** True while {@code player} is close enough to {@code pos} to interact with it. */
    public static boolean isWithinReach(Player player, BlockPos pos) {
        return isWithinReach(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    /**
     * True while {@code blockEntity} is still the live block entity standing at its own position.
     *
     * <p>Mirrors vanilla's {@code Container.stillValidBlockEntity}: identity against the level
     * catches the block being broken, replaced, or its chunk going away, all of which leave a
     * stale instance behind that must not keep a menu alive.
     */
    public static boolean isStillThere(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }
        Level level = blockEntity.getLevel();
        return level != null && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    /**
     * True while the menu should stay open: the block is still there and the player is still in
     * reach of it.
     *
     * <p>{@link net.minecraft.server.level.ServerPlayer} re-checks this every tick, so a broken
     * block closes the menu long before anything could be built in its place.
     */
    public static boolean stillValid(@Nullable BlockEntity blockEntity, Player player) {
        return isStillThere(blockEntity) && isWithinReach(player, blockEntity.getBlockPos());
    }
}
