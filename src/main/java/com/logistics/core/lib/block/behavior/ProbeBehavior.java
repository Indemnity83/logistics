package com.logistics.core.lib.block.behavior;

import com.logistics.core.lib.support.ProbeResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Behavior module for blocks that can be probed/inspected by tools.
 */
public final class ProbeBehavior {

    private ProbeBehavior() {}

    /**
     * Marker interface for blocks that can be probed to display information.
     * Blocks implementing this interface respond to probe tools (like WAILA/HWYLA equivalents).
     */
    public interface Probeable {
        /**
         * Called when a probe tool queries this block.
         *
         * @param level The world
         * @param pos The block position
         * @param player The player using the probe tool
         * @return ProbeResult containing information to display, or null if no information
         */
        ProbeResult onProbe(Level level, BlockPos pos, Player player);
    }
}
