package com.logistics.pipe.network;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
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

    // ===== Supply Operations =====

    /**
     * Register (or refresh) supply for a provider position.
     * Replaces all previous supply entries for this position.
     * Called periodically by provider and crafter modules after scanning their inventories.
     *
     * @param pos      position of the provider pipe
     * @param items    items currently available (ItemVariant → amount); pass empty map to remove
     * @param priority dispatch priority (1 = real stock, 5 = crafter/on-demand)
     */
    void registerSupply(BlockPos pos, Map<ItemVariant, Long> items, int priority);

    /**
     * Get the total available amount of an item across all providers.
     *
     * @param stack Item to query
     * @return Total available amount (Long.MAX_VALUE if any crafter can produce it)
     */
    long getAvailableAmount(ItemStack stack);

    /**
     * Get all available items across all providers.
     *
     * @return Map of items to available amounts
     */
    Map<ItemStack, Long> getAllAvailableItems();

    // ===== Order Operations =====

    /**
     * Place a standing order for items with explicit fulfillment mode.
     * Increments orderedForRequester immediately.
     * The order persists until cancelled or fulfilled via notifyDelivery.
     *
     * @param item            item variant to request
     * @param amount          amount needed
     * @param requester       position of the requesting pipe
     * @param fulfillmentMode whether partial dispatch is acceptable
     * @return UUID of the order (store for cancellation)
     */
    UUID placeOrder(ItemVariant item, long amount, BlockPos requester, FulfillmentMode fulfillmentMode);

    /**
     * Place a standing order with default {@link FulfillmentMode#PARTIAL} fulfillment.
     *
     * @param item      item variant to request
     * @param amount    amount needed
     * @param requester position of the requesting pipe
     * @return UUID of the order (store for cancellation)
     */
    default UUID placeOrder(ItemVariant item, long amount, BlockPos requester) {
        return placeOrder(item, amount, requester, FulfillmentMode.PARTIAL);
    }

    /**
     * Cancel a standing order by ID. Decrements orderedForRequester.
     *
     * @param orderId order to cancel
     */
    void cancelOrder(UUID orderId);

    /**
     * Register a fulfillment-check callback for a dynamic provider (crafter, machine, etc.).
     * The network calls this before dispatching to the provider to validate the ingredient chain.
     *
     * @param pos   position of the provider pipe
     * @param check callback that returns missing ingredients (empty = can fulfill)
     */
    void registerProviderCheck(BlockPos pos, ProviderCanFulfill check);

    /**
     * Remove the fulfillment-check callback for a provider position.
     * Call when the provider can no longer produce its item (recipe cleared, no autocrafter, etc.).
     *
     * @param pos position of the provider pipe
     */
    void unregisterProviderCheck(BlockPos pos);

    /**
     * Dry-run check: returns empty list if the network can satisfy {@code amount} of
     * {@code item} (from stock + dynamic providers, recursively). Otherwise returns
     * the terminal missing ingredient(s).
     *
     * <p>Creates a fresh shared-reservation context for the whole validation tree so that
     * sibling branches that both need the same raw material see the combined deduction.
     *
     * @param item   item to check
     * @param amount amount needed
     * @return empty list if satisfiable; otherwise the root missing ingredient(s)
     */
    List<ItemVariant> getMissingIngredients(ItemVariant item, long amount);

    /**
     * Get the total amount of an item currently ordered for a requester but not yet delivered.
     * Used by supplier/requester pipes to avoid placing duplicate orders.
     *
     * @param requester Position of the requester
     * @param stack     Item to query
     * @return Total ordered amount for this requester that has not yet been delivered
     */
    long getOrderedAmountFor(BlockPos requester, ItemStack stack);

    /**
     * Record physical delivery of items to a requester inventory.
     * Decrements orderedForRequester so that suppliers know the stock has arrived.
     * Called by PipeRuntime when a TravelingItem enters a storage.
     *
     * @param requester destination the item was delivered to
     * @param item      item variant delivered
     * @param amount    amount delivered
     */
    void notifyDelivery(BlockPos requester, ItemVariant item, long amount);

    // ===== Crafter Snapshot =====

    /**
     * Register (or replace) a crafter buffer snapshot for use by the request planner.
     * Called by crafter modules each scan cycle so the planner sees current buffer capacity.
     * Default no-op — implementations override when snapshot storage is available.
     *
     * @param pos      crafting pipe position
     * @param snapshot current crafter state (recipe, output count, buffer capacity)
     */
    default void registerCrafterSnapshot(BlockPos pos, CrafterSnapshot snapshot) {}

    /**
     * Remove the crafter snapshot for a position (e.g. when recipe is cleared or pipe removed).
     * Default no-op — implementations override when snapshot storage is available.
     *
     * @param pos crafting pipe position
     */
    default void unregisterCrafterSnapshot(BlockPos pos) {}

    // ===== Sink Operations =====

    /**
     * Register a position as an item-specific sink with priority.
     * Used by filter, enchantment, polymorphic, and passive-supplier sinks.
     * Priority table: 0=default-route, 1=mod-based, 2=enchantment, 4=terminus,
     *                 5=item-sink filter, 10=passive-supplier.
     *
     * @param pos      Position of the sink
     * @param priority Sink priority (higher = preferred; 0 = lowest)
     */
    void registerSink(BlockPos pos, int priority);

    /**
     * Unregister an item-specific sink.
     *
     * @param pos Position of the sink to remove
     */
    void unregisterSink(BlockPos pos);

    /**
     * Register a position as a default-route (catch-all) sink at priority 0.
     * Convenience for {@code registerSink(sink, 0)}.
     */
    default void registerDefaultRouteSink(BlockPos sink) {
        registerSink(sink, 0);
    }

    /**
     * Unregister a default-route sink.
     * Convenience for {@code unregisterSink(sink)}.
     */
    default void unregisterDefaultRouteSink(BlockPos sink) {
        unregisterSink(sink);
    }

    /**
     * Declare that the sink at {@code pos} wants to receive items of this specific type.
     * Called by filtered sinks (SinkModule filter slots, PassiveSupplierModule config items).
     * These are indexed so only interested sinks are considered during lookup.
     */
    void registerSinkInterest(BlockPos pos, Item item);

    /**
     * Remove all specific-item interests for a sink position.
     * Call before re-registering interests (e.g., when filter changes) or on detach.
     */
    void unregisterSinkInterests(BlockPos pos);

    /**
     * Declare that the sink at {@code pos} wants to be considered for ALL items.
     * Used by default-route, enchantment, and polymorphic sinks.
     */
    void registerGenericSinkInterest(BlockPos pos);

    /**
     * Remove generic interest for a sink position.
     */
    void unregisterGenericSinkInterest(BlockPos pos);

    /**
     * Find a sink for an item stack.
     * Checks filtered sinks first, then falls back to default routes.
     *
     * @param stack Item to find sink for
     * @return Position of sink, or null if none found
     */
    @Nullable
    BlockPos findSinkFor(ItemStack stack);

    /**
     * Find a filtered sink for an item stack (no default-route fallback).
     * Only returns pipes that have an explicit item filter matching the stack.
     * Used by QuickSort to avoid dumping items into catch-all default routes.
     *
     * @param stack Item to find sink for
     * @return Position of a filtered sink, or null if none found
     */
    @Nullable
    default BlockPos findFilteredSinkFor(ItemStack stack) {
        throw new UnsupportedOperationException(
                "findFilteredSinkFor must be overridden — default fallback to findSinkFor would include default-route sinks");
    }

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
