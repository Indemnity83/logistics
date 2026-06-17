package com.logistics.core.lib.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Behavior module for blocks that can be wrenched/rotated by tools.
 */
public final class WrenchBehavior {

    private WrenchBehavior() {}

    /**
     * Marker interface for blocks that can be interacted with using a wrench tool.
     * Blocks implementing this interface can be rotated, configured, or otherwise
     * modified by wrench-like tools.
     */
    public interface Wrenchable {
        /**
         * Called when a player uses a wrench on this block.
         * Typically used to rotate the block or change its configuration.
         *
         * @param level The world
         * @param pos The block position
         * @param player The player using the wrench
         * @return The interaction result (SUCCESS if handled, PASS otherwise)
         */
        InteractionResult onWrench(Level level, BlockPos pos, Player player);

        /**
         * Called when a player uses a wrench on a specific face of this block.
         *
         * <p>Defaults to the face-agnostic {@link #onWrench(Level, BlockPos, Player)} so existing
         * wrenchable blocks need no changes. Blocks that act per-face (e.g. toggling a pipe side
         * connection) override this variant to use {@code face}.
         *
         * @param face the block face the player clicked
         */
        default InteractionResult onWrench(Level level, BlockPos pos, Player player, Direction face) {
            return onWrench(level, pos, player);
        }
    }

    /**
     * Try to wrench the block at the given position.
     * Only works if the block implements Wrenchable.
     *
     * <p>This is intended for use in wrench item implementations
     * to conditionally handle wrench interactions only for blocks that support them.
     *
     * @param level The world
     * @param pos The block position
     * @param player The player using the wrench
     * @return The result from onWrench if block is Wrenchable, PASS otherwise
     */
    public static InteractionResult tryWrench(Level level, BlockPos pos, Player player) {
        Block block = level.getBlockState(pos).getBlock();

        if (block instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(level, pos, player);
        }

        return InteractionResult.PASS;
    }

    /**
     * Try to wrench the block at the given position, passing the clicked face.
     * Only works if the block implements Wrenchable.
     *
     * @param face the block face the player clicked
     * @return the result from onWrench if block is Wrenchable, PASS otherwise
     */
    public static InteractionResult tryWrench(Level level, BlockPos pos, Player player, Direction face) {
        Block block = level.getBlockState(pos).getBlock();

        if (block instanceof Wrenchable wrenchable) {
            return wrenchable.onWrench(level, pos, player, face);
        }

        return InteractionResult.PASS;
    }
}
