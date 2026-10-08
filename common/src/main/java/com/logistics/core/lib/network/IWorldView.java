package com.logistics.core.lib.network;

import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

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
     * Whether the inventory behind the sink at {@code pos} can take the whole stack right now.
     *
     * <p>A filter match says a sink <em>wants</em> an item; this says it has somewhere to put it.
     * Routing needs both: a pipe hands its cargo to the attached inventory on arrival and drops
     * whatever will not fit, so choosing a full sink spills the remainder on the floor.
     *
     * <p>The whole stack is required rather than any part of it, because a partial insert is
     * exactly what spills — the accepted part lands and the rest is dropped.
     *
     * <p>Defaults to {@code true} so a view that cannot see inventories routes as before.
     */
    default boolean sinkHasRoomFor(BlockPos pos, net.minecraft.world.item.ItemStack stack) {
        return true;
    }

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
     * Ask the provider pipe at {@code provider} to extract and dispatch fluid to {@code requester} —
     * the fluid analogue of {@link #dispatch}. Default no-op ({@code return 0}) so existing
     * {@link IWorldView} test fakes don't need updating; the Minecraft implementation overrides it.
     *
     * @param provider   position of the provider pipe
     * @param requester  position of the destination requester
     * @param fluid      fluid to extract
     * @param amountMb   requested mB
     * @param deliveryId UUID to attach to the minted packet(s) for delivery accounting
     * @return actual mB dispatched (0 if the provider could not fulfill)
     */
    default long dispatchFluid(BlockPos provider, BlockPos requester, Fluid fluid, long amountMb, UUID deliveryId) {
        return 0;
    }

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

    /**
     * Resolve the energy storage exposed by the block at {@code pos}, or {@code null} if there is
     * none (or the chunk is unloaded). Used by the network to draw on registered battery sources.
     */
    @Nullable
    default IEnergyStorage energyStorageAt(BlockPos pos) {
        return null;
    }

    /**
     * Current world game time, in ticks. Used as the cache key for per-tick network queries (e.g.
     * {@code isPowered()}) so they compute once per network per tick rather than once per pipe.
     * No default: a constant value would make those caches never invalidate, so every
     * implementation must supply a real, monotonically advancing tick.
     */
    long gameTime();
}
