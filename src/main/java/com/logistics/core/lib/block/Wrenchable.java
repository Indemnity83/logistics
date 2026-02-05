package com.logistics.core.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Interface for blocks that support wrench interactions.
 *
 * <p>Blocks implementing this interface can handle wrench actions
 * without the wrench needing to know about specific block implementations.
 * The player is passed through so implementers can check {@code player.isSneaking()}
 * if they need different behavior for sneak vs non-sneak interactions.
 */
public interface Wrenchable {

    /**
     * Called when a player right-clicks this block with a wrench.
     *
     * <p>Implementers can check {@code player.isSneaking()} to provide
     * different behavior for sneak vs non-sneak interactions.
     *
     * @param world the world
     * @param pos the block position
     * @param player the player using the wrench
     * @return the action result
     */
    InteractionResult onWrench(Level world, BlockPos pos, Player player);
}
