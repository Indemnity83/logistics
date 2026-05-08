package com.logistics.core.lib.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

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
     * Check whether the pipe at the given position has a sink module whose filter accepts the item.
     * Returns false if the position has no pipe, no sink module, or the filter does not match.
     */
    boolean matchesSinkFilter(BlockPos pos, net.minecraft.world.item.ItemStack stack);

    /**
     * Ask the provider pipe at {@code provider} to extract and dispatch items to {@code requester}.
     * Finds the pipe's modules and calls {@code onDispatch()} on the first module that can fulfill.
     *
     * @param provider   position of the provider pipe
     * @param requester  position of the destination requester
     * @param item       item key to extract
     * @param amount     requested amount
     * @param deliveryId UUID to attach to the TravelingItem for delivery accounting
     * @return actual amount dispatched (0 if provider could not fulfill)
     */
    long dispatch(BlockPos provider, BlockPos requester, IItemKey item, long amount, UUID deliveryId);

    /**
     * Check if this is a client-side world.
     */
    boolean isClientSide();

    /**
     * Send an alert message to all players within 64 blocks of {@code pos}.
     * No-op on the client side.
     *
     * @param pos     position of the event (used for proximity filtering)
     * @param message message to broadcast
     */
    void broadcastAlert(BlockPos pos, Component message);
}
