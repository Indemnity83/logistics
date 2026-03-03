package com.logistics.core.lib.network;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Main network interface that modules depend on.
 * Applies Dependency Inversion Principle - modules depend on this abstraction,
 * not on the concrete PipeNetwork implementation.
 */
public interface ILogisticsNetwork {
    /**
     * Get the unique identifier for this network.
     */
    UUID getId();

    // ===== Provider Operations =====

    /**
     * Update the provider cache for a position.
     *
     * @param pos Position of the provider
     * @param items Available items at this provider
     * @param gameTime Current game time for staleness tracking
     */
    void updateProviderCache(BlockPos pos, Map<ItemStack, Long> items, long gameTime);

    /**
     * Update the crafter cache for a crafting pipe position.
     * Advertises craftable items as available in the network.
     *
     * @param pos Position of the crafting pipe
     * @param craftableItems Items this pipe can craft (typically Long.MAX_VALUE per item)
     * @param gameTime Current game time for staleness tracking
     */
    void updateCrafterCache(BlockPos pos, Map<ItemStack, Long> craftableItems, long gameTime);

    /**
     * Check if a position is registered as a crafting provider.
     *
     * @param pos Position to check
     * @return true if position has an active crafter cache
     */
    boolean isCraftingProvider(BlockPos pos);

    /**
     * Get the total available amount of an item across all providers.
     *
     * @param stack Item to query
     * @return Total available amount
     */
    long getAvailableAmount(ItemStack stack);

    /**
     * Get all available items across all providers.
     *
     * @return Map of items to available amounts
     */
    Map<ItemStack, Long> getAllAvailableItems();

    // ===== Request Operations =====

    /**
     * Add a request to the network.
     *
     * @param request Request to add
     */
    void addRequest(ItemRequest request);

    /**
     * Add an order for a provider to fulfill.
     *
     * @param order Order to add
     */
    void addOrder(LogisticsOrder order);

    /**
     * Get the total amount of an item currently ordered for a requester but not yet shipped.
     * Used by supplier pipes to avoid duplicate requests without fragile NBT pending tracking.
     *
     * @param requester Position of the requester (e.g. supplier pipe)
     * @param stack Item to query
     * @return Total ordered amount currently in the order queue for this requester
     */
    long getOrderedAmountFor(BlockPos requester, ItemStack stack);

    /**
     * Get all pending orders for a provider.
     *
     * @param provider Position of the provider
     * @return List of orders waiting to be fulfilled
     */
    List<LogisticsOrder> getOrdersFor(BlockPos provider);

    /**
     * Remove a completed or superseded order from the network.
     *
     * @param order Order to remove
     */
    void removeOrder(LogisticsOrder order);

    /**
     * Transition a pending order to in-transit state when the provider ships items.
     * Removes the pending order, requeues any remainder, and returns an in-transit record
     * to attach to the TravelingItem. orderedForRequester is NOT decremented — it stays
     * positive until confirmDelivery is called.
     *
     * @param order Pending order that is being fulfilled
     * @param shippedAmount Actual amount extracted (≤ order.amount())
     * @return In-transit order to attach to the TravelingItem
     */
    LogisticsOrder markShipped(LogisticsOrder order, long shippedAmount, long gameTime);

    /**
     * Confirm physical delivery of an in-transit item to an inventory.
     * Decrements orderedForRequester so suppliers know the stock has arrived.
     *
     * @param order In-transit order returned by markShipped
     */
    void confirmDelivery(LogisticsOrder order);

    // ===== Sink Operations =====

    /**
     * Register a position as a default route sink with priority.
     *
     * @param sink Position of the sink
     * @param priority Sink priority (higher = preferred)
     */
    void registerDefaultRouteSink(BlockPos sink, int priority);

    /**
     * Unregister a default route sink.
     *
     * @param sink Position of the sink
     */
    void unregisterDefaultRouteSink(BlockPos sink);

    /**
     * Find a sink for an item stack.
     * Checks filtered sinks first, then falls back to default routes.
     *
     * @param stack Item to find sink for
     * @return Position of sink, or null if none found
     */
    @Nullable
    BlockPos findSinkFor(ItemStack stack);

    // ===== Routing Operations =====

    /**
     * Get next hop direction from current position toward destination.
     *
     * @param current Current position
     * @param destination Target position
     * @return Direction to travel, or null if no path exists
     */
    @Nullable
    Direction getNextHop(BlockPos current, BlockPos destination);

    /**
     * Find shortest path between two positions.
     *
     * @param start Starting position
     * @param goal Goal position
     * @return List of positions from start to goal, or null if no path exists
     */
    @Nullable
    List<BlockPos> findPath(BlockPos start, BlockPos goal);

    // ===== Graph Operations =====

    /**
     * Check if the network contains a position.
     *
     * @param pos Position to check
     * @return True if position is in network
     */
    boolean contains(BlockPos pos);

    /**
     * Get all member positions in the network.
     *
     * @return Set of all positions
     */
    Set<BlockPos> getMembers();

    /**
     * Get the size of the network.
     *
     * @return Number of pipes in network
     */
    int size();
}
