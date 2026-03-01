package com.logistics.core.lib.network;

import com.logistics.pipe.modules.Module;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction for world/pipe queries to enable testing.
 * Minecraft implementation uses Level/BlockEntity.
 * Test implementation uses HashMap-based fake.
 */
public interface IWorldView {
    /**
     * Check if a block at the given position is a pipe.
     */
    boolean isPipe(BlockPos pos);

    /**
     * Get all connected neighbor pipes for a given position.
     * Only returns neighbors where pipes are actually connected.
     */
    List<BlockPos> getConnectedNeighbors(BlockPos pos);

    /**
     * Query pipe modules at a position.
     * Returns null if not a pipe or module not found.
     */
    @Nullable
    <T extends Module> T getModule(BlockPos pos, Class<T> moduleClass);

    /**
     * Check whether the pipe at the given position has a sink module whose filter accepts the item.
     * Returns false if the position has no pipe, no sink module, or the filter does not match.
     */
    boolean matchesSinkFilter(BlockPos pos, net.minecraft.world.item.ItemStack stack);

    /**
     * Check if this is a client-side world.
     */
    boolean isClientSide();
}
