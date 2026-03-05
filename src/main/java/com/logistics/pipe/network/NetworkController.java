package com.logistics.pipe.network;

import com.logistics.core.lib.network.Order;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Core network dispatch logic. Replaces RequestMatcher.
 *
 * <p>Manages a supply table (what providers have), a standing order queue (what requesters want),
 * and per-requester ordered-amount tracking (how much is already on the way).
 *
 * <p>Dispatch is synchronous: the network calls {@link #nextDispatchable()} each tick,
 * then calls the provider's {@code onDispatch()} directly via {@link com.logistics.core.lib.network.IWorldView}.
 * Supply is immediately updated after extraction so subsequent orders in the same tick see accurate stock.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class NetworkController {

    /** Mutable supply entry for a single provider position. */
    private static class SupplyEntry {
        final BlockPos pos;
        long available;
        final int priority;

        SupplyEntry(BlockPos pos, long available, int priority) {
            this.pos = pos;
            this.available = available;
            this.priority = priority;
        }

        SupplyEntry copy() {
            return new SupplyEntry(pos, available, priority);
        }
    }

    /**
     * Command returned by {@link #nextDispatchable()} describing what the network wants dispatched.
     */
    public record DispatchCommand(
            UUID orderId, BlockPos provider, BlockPos requester, ItemVariant item, long amount) {}

    // Per-item supply entries, sorted by priority ascending (1 = real stock first, 5 = crafter fallback)
    private final Map<ItemVariant, List<SupplyEntry>> supplyTable = new HashMap<>();

    // Standing orders in insertion order (FIFO)
    private final Map<UUID, Order> orderQueue = new LinkedHashMap<>();

    // How many items each requester has outstanding (ordered but not yet delivered)
    private final Map<BlockPos, Map<ItemVariant, Long>> orderedForRequester = new HashMap<>();

    /**
     * Register (or replace) supply for a provider position.
     * Replaces all previous supply entries for this position with the new inventory snapshot.
     *
     * @param pos      provider position
     * @param items    current available items (ItemVariant → amount)
     * @param priority dispatch priority (lower = preferred; 1 = real stock, 5 = crafter)
     */
    public void registerSupply(BlockPos pos, Map<ItemVariant, Long> items, int priority) {
        // Remove all existing entries for this position
        for (List<SupplyEntry> entries : supplyTable.values()) {
            entries.removeIf(e -> e.pos.equals(pos));
        }
        supplyTable.entrySet().removeIf(e -> e.getValue().isEmpty());

        // Add fresh entries, maintaining sorted order by priority
        for (Map.Entry<ItemVariant, Long> entry : items.entrySet()) {
            if (entry.getValue() <= 0) continue;
            List<SupplyEntry> list = supplyTable.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            list.add(new SupplyEntry(pos, entry.getValue(), priority));
            list.sort(Comparator.comparingInt(e -> e.priority));
        }
    }

    /**
     * Remove all supply entries for a position (e.g., when a provider pipe is removed).
     */
    public void removeSupply(BlockPos pos) {
        for (List<SupplyEntry> entries : supplyTable.values()) {
            entries.removeIf(e -> e.pos.equals(pos));
        }
        supplyTable.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Place a standing order. Increments orderedForRequester immediately.
     *
     * @return UUID of the new order (store for later cancellation)
     */
    public UUID placeOrder(ItemVariant item, long amount, BlockPos requester) {
        UUID id = UUID.randomUUID();
        orderQueue.put(id, new Order(id, item, amount, requester));
        orderedForRequester
                .computeIfAbsent(requester, k -> new HashMap<>())
                .merge(item, amount, Long::sum);
        return id;
    }

    /**
     * Cancel a standing order by ID. Decrements orderedForRequester.
     */
    public void cancelOrder(UUID id) {
        Order order = orderQueue.remove(id);
        if (order != null) {
            decrementOrdered(order.requester(), order.item(), order.amount());
        }
    }

    /**
     * Cancel all standing orders for a requester and clear their orderedForRequester entries.
     * Used when a pipe is removed from the network.
     */
    public void cancelOrdersFor(BlockPos requester) {
        orderQueue.entrySet().removeIf(e -> e.getValue().requester().equals(requester));
        orderedForRequester.remove(requester);
    }

    /**
     * Find the next dispatchable order: an order whose full amount is available from a single
     * supply entry. Reserves (decrements) that supply immediately so subsequent calls in the
     * same tick see accurate remaining stock.
     *
     * @return dispatch command, or null if no order can be fulfilled right now
     */
    public DispatchCommand nextDispatchable() {
        for (Map.Entry<UUID, Order> entry : orderQueue.entrySet()) {
            Order order = entry.getValue();
            List<SupplyEntry> entries = supplyTable.get(order.item());
            if (entries == null || entries.isEmpty()) continue;

            for (int i = 0; i < entries.size(); i++) {
                SupplyEntry supply = entries.get(i);
                if (supply.available >= order.amount()) {
                    // Reserve supply (unlimited sources like crafters keep Long.MAX_VALUE)
                    if (supply.available != Long.MAX_VALUE) {
                        supply.available -= order.amount();
                    }
                    if (supply.available == 0) {
                        entries.remove(i);
                        if (entries.isEmpty()) supplyTable.remove(order.item());
                    }
                    return new DispatchCommand(
                            order.id(), supply.pos, order.requester(), order.item(), order.amount());
                }
            }
        }
        return null;
    }

    /**
     * Record that a dispatch was completed. Removes or reduces the standing order.
     * Does NOT touch orderedForRequester — that only decrements on physical delivery via notifyDelivery.
     *
     * @param orderId UUID of the order that was dispatched
     * @param shipped actual amount dispatched (≤ order amount)
     */
    public void recordDispatched(UUID orderId, long shipped) {
        Order order = orderQueue.get(orderId);
        if (order == null) return;
        if (shipped >= order.amount()) {
            orderQueue.remove(orderId);
        } else {
            orderQueue.put(orderId, new Order(orderId, order.item(), order.amount() - shipped, order.requester()));
        }
    }

    /**
     * Remove all supply entries for a provider that returned 0 items this tick.
     * Prevents repeated dispatch attempts to a provider that can't currently extract.
     * The provider will re-register supply on its next scan cycle.
     */
    public void markSupplyUnavailable(BlockPos provider) {
        for (List<SupplyEntry> entries : supplyTable.values()) {
            entries.removeIf(e -> e.pos.equals(provider));
        }
        supplyTable.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Record physical delivery of items to a requester. Decrements orderedForRequester.
     * Called by PipeRuntime when a TravelingItem enters an inventory.
     */
    public void notifyDelivery(BlockPos requester, ItemVariant item, long amount) {
        decrementOrdered(requester, item, amount);
    }

    /**
     * Get how many items are currently ordered (in-flight or pending) for a requester.
     * Used by suppliers/requesters to avoid placing duplicate orders.
     */
    public long getOrderedAmountFor(BlockPos requester, ItemVariant item) {
        Map<ItemVariant, Long> map = orderedForRequester.get(requester);
        return map == null ? 0L : map.getOrDefault(item, 0L);
    }

    /**
     * Get total available supply for an item across all providers.
     * Returns Long.MAX_VALUE if any entry has unlimited supply (e.g., crafters).
     */
    public long getAvailableAmount(ItemVariant item) {
        List<SupplyEntry> entries = supplyTable.get(item);
        if (entries == null || entries.isEmpty()) return 0L;
        long total = 0;
        for (SupplyEntry e : entries) {
            if (e.available == Long.MAX_VALUE) return Long.MAX_VALUE;
            total += e.available;
            if (total < 0) return Long.MAX_VALUE; // overflow guard
        }
        return total;
    }

    /**
     * Get all items with available supply, for UI/network display.
     */
    public Map<ItemStack, Long> getAllAvailableItems() {
        Map<ItemStack, Long> result = new HashMap<>();
        for (Map.Entry<ItemVariant, List<SupplyEntry>> entry : supplyTable.entrySet()) {
            long total = 0;
            for (SupplyEntry e : entry.getValue()) {
                if (e.available == Long.MAX_VALUE) {
                    total = Long.MAX_VALUE;
                    break;
                }
                total += e.available;
                if (total < 0) {
                    total = Long.MAX_VALUE;
                    break;
                }
            }
            if (total > 0) {
                result.merge(entry.getKey().toStack(1), total, Long::sum);
            }
        }
        return result;
    }

    /**
     * Merge another NetworkController into this one (used when two networks join).
     */
    public void merge(NetworkController other) {
        // Merge supply tables (re-sort by priority after combining)
        for (Map.Entry<ItemVariant, List<SupplyEntry>> entry : other.supplyTable.entrySet()) {
            List<SupplyEntry> myList = supplyTable.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            for (SupplyEntry e : entry.getValue()) {
                myList.add(e.copy());
            }
            myList.sort(Comparator.comparingInt(e -> e.priority));
        }

        // Merge order queues (preserve FIFO; skip duplicates by UUID)
        for (Map.Entry<UUID, Order> entry : other.orderQueue.entrySet()) {
            orderQueue.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Merge orderedForRequester
        for (Map.Entry<BlockPos, Map<ItemVariant, Long>> entry : other.orderedForRequester.entrySet()) {
            Map<ItemVariant, Long> myMap =
                    orderedForRequester.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            entry.getValue().forEach((variant, amount) -> myMap.merge(variant, amount, Long::sum));
        }
    }

    private void decrementOrdered(BlockPos requester, ItemVariant item, long amount) {
        Map<ItemVariant, Long> map = orderedForRequester.get(requester);
        if (map != null) {
            long remaining = map.merge(item, -amount, Long::sum);
            if (remaining <= 0) map.remove(item);
        }
    }
}
