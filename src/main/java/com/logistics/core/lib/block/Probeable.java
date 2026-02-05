package com.logistics.core.lib.block;

import com.logistics.core.lib.support.ProbeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for blocks that support probe interactions.
 *
 * <p>Blocks implementing this interface can provide diagnostic information
 * to the player when probed, without the probe item needing to know about
 * specific block implementations.
 */
public interface Probeable {

    /**
     * Called when a player right-clicks this block with a probe.
     *
     * <p>Implementations should return a {@link ProbeResult} containing
     * diagnostic information to display to the player. The ProbeItem
     * handles the actual rendering of the result.
     *
     * @param world the world
     * @param pos the block position
     * @param player the player using the probe
     * @return the probe result, or null if this block cannot be probed
     */
    @Nullable ProbeResult onProbe(Level world, BlockPos pos, Player player);
}
