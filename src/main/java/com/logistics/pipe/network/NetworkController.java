package com.logistics.pipe.network;

import com.logistics.core.lib.network.IngredientChecker;
import com.logistics.core.lib.network.Order;
import com.logistics.core.lib.network.ProviderCanFulfill;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

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
 * <p>Before dispatching to a dynamic provider (crafter, etc.), a dry-run ingredient-chain check
 * validates that all required raw materials are actually available. The check uses a shared
 * "claimed" scratch-pad so that sibling branches that need the same ingredient see the combined
 * deduction. Orders whose chain cannot be satisfied are cancelled and the failure listener is
 * notified.
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

    /**
     * Listener notified when an order is cancelled because its ingredient chain cannot be
     * satisfied. Called synchronously inside {@link #nextDispatchable()}.
     */
    @FunctionalInterface
    public interface OrderFailureListener {
        void onOrderFailed(UUID orderId, BlockPos requester, ItemVariant item,
                           long amount, List<ItemVariant> missing);
    }

    // Per-item supply entries, sorted by priority ascending (1 = real stock first, 5 = crafter fallback)
    private final Map<ItemVariant, List<SupplyEntry>> supplyTable = new HashMap<>();

    // Standing orders in insertion order (FIFO)
    private final Map<UUID, Order> orderQueue = new LinkedHashMap<>();

    // How many items each requester has outstanding (ordered but not yet delivered)
    private final Map<BlockPos, Map<ItemVariant, Long>> orderedForRequester = new HashMap<>();

    // Fulfillment-check callbacks for dynamic providers (crafters, machines, etc.)
    private final Map<BlockPos, ProviderCanFulfill> providerChecks = new HashMap<>();

    @Nullable
    private OrderFailureListener failureListener;

    // ===== Provider Check Registration =====

    public void registerProviderCheck(BlockPos pos, ProviderCanFulfill check) {
        providerChecks.put(pos, check);
    }

    public void unregisterProviderCheck(BlockPos pos) {
        providerChecks.remove(pos);
    }

    public void setOrderFailureListener(@Nullable OrderFailureListener listener) {
        this.failureListener = listener;
    }

    // ===== Supply Registration =====

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
            if (entry.getValue() < 0) continue;
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

    // ===== Order Management =====

    /**
     * Place a standing order. Increments orderedForRequester immediately.
     *
     * @return UUID of the new order (store for later cancellation)
     */
    public UUID placeOrder(ItemVariant item, long amount, BlockPos requester) {
        if (amount <= 0) throw new IllegalArgumentException("Order amount must be positive, got: " + amount);
        UUID id = UUID.randomUUID();
        orderQueue.put(id, new Order(id, item, amount, requester));
        orderedForRequester
                .computeIfAbsent(requester, k -> new HashMap<>())
                .merge(item, amount, Long::sum);
        NetDbg.out("Order placed: {} | {}x {} → {} (requester)",
                id.toString().substring(0, 8), amount, item.toStack().getItem(), requester);
        return id;
    }

    /**
     * Cancel a standing order by ID. Decrements orderedForRequester.
     */
    public void cancelOrder(UUID id) {
        Order order = orderQueue.remove(id);
        if (order != null) {
            NetDbg.out("Order cancelled: {}", id.toString().substring(0, 8));
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

    // ===== Dispatch =====

    /**
     * Find the next dispatchable order. Performs a pre-validation ingredient-chain check before
     * dispatching to any dynamic provider (crafter) or when real stock is only a partial fill
     * and a crafter would be needed for the remainder.
     *
     * <p>If the chain check fails the order is cancelled and the failure listener is notified.
     *
     * @return dispatch command, or null if no order can be fulfilled right now
     */
    public DispatchCommand nextDispatchable() {
        Iterator<Map.Entry<UUID, Order>> orderIt = orderQueue.entrySet().iterator();
        while (orderIt.hasNext()) {
            Order order = orderIt.next().getValue();
            List<SupplyEntry> entries = supplyTable.get(order.item());
            if (entries == null || entries.isEmpty()) continue;
            DispatchCommand cmd = tryDispatch(orderIt, order, entries);
            if (cmd != null) return cmd;
        }
        return null;
    }

    /**
     * Attempt to create a dispatch command for a single order given its current supply entries.
     * Always operates on the first (highest-priority) entry in the list; the list is sorted by
     * priority so real stock (priority 1) always precedes crafters (priority 5).
     * Validates the ingredient chain before committing to any crafter or partial-stock dispatch.
     *
     * @return a dispatch command if ready, or null if the order cannot be dispatched now
     *         (either no matching supply, or the order was cancelled due to a missing ingredient)
     */
    @Nullable
    private DispatchCommand tryDispatch(
            Iterator<Map.Entry<UUID, Order>> orderIt, Order order, List<SupplyEntry> entries) {
        SupplyEntry supply = entries.getFirst();

        if (supply.available == 0) {
            // On-demand supply (crafter): validate ingredient chain before dispatching
            List<ItemVariant> missing = getMissingIngredients(order.item(), order.amount());
            if (!missing.isEmpty()) {
                cancelAndNotify(orderIt, order, missing);
                return null;
            }
            return new DispatchCommand(
                    order.id(), supply.pos, order.requester(), order.item(), order.amount());
        }

        if (supply.available >= order.amount()) {
            // Full fill from real stock: no pre-check needed
            supply.available -= order.amount();
            if (supply.available == 0) {
                entries.removeFirst();
                if (entries.isEmpty()) supplyTable.remove(order.item());
            }
            return new DispatchCommand(
                    order.id(), supply.pos, order.requester(), order.item(), order.amount());
        }

        // Partial fill from real stock — pre-check only when a crafter also covers this item
        // (the crafter will be needed to fill the remainder on a subsequent tick)
        boolean crafterExists = entries.stream().anyMatch(e -> e.available == 0);
        if (crafterExists) {
            List<ItemVariant> missing = getMissingIngredients(order.item(), order.amount());
            if (!missing.isEmpty()) {
                cancelAndNotify(orderIt, order, missing);
                return null;
            }
        }

        // Pre-check passed (or no crafter covers this item): dispatch partial stock now
        long partial = supply.available;
        supply.available = 0;
        entries.removeFirst();
        if (entries.isEmpty()) supplyTable.remove(order.item());
        return new DispatchCommand(
                order.id(), supply.pos, order.requester(), order.item(), partial);
    }

    /**
     * Record that a dispatch was completed. Removes or reduces the standing order.
     * Does NOT touch orderedForRequester — that only decrements on physical delivery via notifyDelivery.
     *
     * @param orderId UUID of the order that was dispatched
     * @param shipped actual amount dispatched (≤ order amount)
     */
    public void recordDispatched(UUID orderId, long shipped) {
        if (shipped <= 0) return;
        Order order = orderQueue.get(orderId);
        if (order == null) return;
        NetDbg.out("Recorded dispatch: {} | {} items shipped", orderId.toString().substring(0, 8), shipped);
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
        if (amount <= 0) return;
        NetDbg.out("Delivery notified: {} received {}x {}", requester, amount, item.toStack().getItem());
        decrementOrdered(requester, item, amount);
    }

    // ===== Ingredient Chain Validation =====

    /**
     * Dry-run check: returns empty list if the network can satisfy {@code amount} of {@code item}
     * (from real stock + dynamic providers, recursively). Otherwise returns the terminal missing
     * ingredient(s). Creates a fresh shared-reservation context for the whole validation tree.
     */
    public List<ItemVariant> getMissingIngredients(ItemVariant item, long amount) {
        Map<ItemVariant, Long> claimed = new HashMap<>();
        Set<ItemVariant> visited = new HashSet<>();
        return collectMissing(item, amount, claimed, visited);
    }

    /**
     * Recursive dry-run that walks the ingredient tree.
     *
     * <p>{@code claimed} is a shared scratch-pad: as each branch "uses" stock during the check,
     * the amounts are deducted from effective availability so sibling branches that need the same
     * material see reduced stock. It is never written back to the real supply table.
     *
     * <p>{@code visited} is a DFS path guard against ingredient cycles. Items are removed on
     * exit from the {@code finally} block so they can appear in separate branches.
     */
    private List<ItemVariant> collectMissing(
            ItemVariant item, long amount,
            Map<ItemVariant, Long> claimed, Set<ItemVariant> visited) {
        if (!visited.add(item)) return List.of(); // cycle guard: optimistically assume satisfiable

        try {
            // Sum real stock across all provider entries for this item
            long realStock = 0;
            List<SupplyEntry> entries = supplyTable.get(item);
            if (entries != null) {
                for (SupplyEntry e : entries) {
                    if (e.available > 0) realStock += e.available;
                }
            }

            long alreadyClaimed = claimed.getOrDefault(item, 0L);
            long effectiveStock = Math.max(0L, realStock - alreadyClaimed);

            if (effectiveStock >= amount) {
                // Enough real stock: reserve in the dry-run and succeed
                claimed.merge(item, amount, Long::sum);
                return List.of();
            }

            // Partially covered by real stock: consume what's available and check providers for rest
            long remaining = amount - effectiveStock;
            if (effectiveStock > 0) {
                claimed.merge(item, effectiveStock, Long::sum);
            }

            // Try the first dynamic provider (crafter) that covers this item
            if (entries != null) {
                for (SupplyEntry e : entries) {
                    if (e.available != 0) continue; // skip real-stock entries
                    ProviderCanFulfill check = providerChecks.get(e.pos);
                    if (check == null) return List.of(); // no check = backward compat: assume OK
                    IngredientChecker checker = (ing, amt) ->
                            collectMissing(ItemVariant.of(ing), amt, claimed, visited);
                    List<ItemVariant> missing = check.getMissing(remaining, checker);
                    if (missing.isEmpty()) return List.of();
                    return missing; // return first failure; only one provider is tried
                }
            }

            // No dynamic provider can cover the remainder
            return List.of(item);
        } finally {
            visited.remove(item); // allow item to be revisited in sibling branches
        }
    }

    // ===== Query =====

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
     * Returns Long.MAX_VALUE if any crafter entry (available == 0) can produce it on demand,
     * since the item can always be crafted. Otherwise returns the sum of real-stock amounts.
     */
    public long getAvailableAmount(ItemVariant item) {
        List<SupplyEntry> entries = supplyTable.get(item);
        if (entries == null || entries.isEmpty()) return 0L;
        long total = 0;
        for (SupplyEntry e : entries) {
            if (e.available == 0) return Long.MAX_VALUE; // crafter: on-demand, effectively unlimited
            total += e.available;
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
                total += e.available; // 0 for crafters; positive for real stock
            }
            result.merge(entry.getKey().toStack(1), total, Long::sum);
        }
        return result;
    }

    // ===== Merge =====

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

        // Merge provider checks
        providerChecks.putAll(other.providerChecks);
    }

    // ===== Helpers =====

    private void cancelAndNotify(
            Iterator<Map.Entry<UUID, Order>> orderIt, Order order, List<ItemVariant> missing) {
        orderIt.remove();
        decrementOrdered(order.requester(), order.item(), order.amount());
        NetDbg.out("Order failed (missing ingredients): {} | {}x {} → {} | missing: {}",
                order.id().toString().substring(0, 8), order.amount(),
                order.item().toStack().getItem(), order.requester(), missing);
        if (failureListener != null) {
            failureListener.onOrderFailed(
                    order.id(), order.requester(), order.item(), order.amount(), missing);
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
