package com.logistics.pipe.network;

import com.logistics.LogisticsMod;
import com.logistics.core.lib.network.*;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.ItemStorageLookup;
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
 * then calls the provider's {@code onDispatch()} directly via {@link com.logistics.core.lib.network.IWorldView IWorldView}.
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
public class NetworkController implements PlanningView {

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
            UUID orderId, BlockPos provider, BlockPos requester, IItemKey item, long amount) {}

    /**
     * Listener notified when an order is cancelled because its ingredient chain cannot be
     * satisfied. Called synchronously inside {@link #nextDispatchable()}.
     */
    @FunctionalInterface
    public interface OrderFailureListener {
        void onOrderFailed(UUID orderId, BlockPos requester, IItemKey item,
                           long amount, List<IItemKey> missing);
    }

    // Per-item supply entries, sorted by priority ascending (1 = real stock first, 5 = crafter fallback)
    private final Map<IItemKey, List<SupplyEntry>> supplyTable = new HashMap<>();

    // Standing orders in insertion order (FIFO)
    private final Map<UUID, Order> orderQueue = new LinkedHashMap<>();

    // Dispatched orders that are physically traveling and can be retried if delivery fails.
    private final Map<UUID, Order> inTransitOrders = new HashMap<>();

    // How many items each requester has outstanding (ordered but not yet delivered)
    private final Map<BlockPos, Map<IItemKey, Long>> orderedForRequester = new HashMap<>();

    // Fulfillment-check callbacks for dynamic providers (crafters, machines, etc.)
    private final Map<BlockPos, ProviderCanFulfill> providerChecks = new HashMap<>();

    // Explicit reservation tracking: replaces implicit SupplyEntry.available mutation
    private final ReservationManager reservationManager = new ReservationManager();

    // Crafter buffer snapshots registered by CraftingModule each scan cycle
    private final Map<BlockPos, CrafterSnapshot> crafterSnapshots = new HashMap<>();

    // Providers skipped this tick because they returned a deferred result (e.g. buffer full).
    // Cleared at the start of each network tick via clearDeferredProviders().
    private final Set<BlockPos> deferredProviders = new HashSet<>();

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
     * @param items    current available items (IItemKey → amount)
     * @param priority dispatch priority (lower = preferred; 1 = real stock, 5 = crafter)
     */
    public void registerSupply(BlockPos pos, Map<IItemKey, Long> items, int priority) {
        // Remove all existing entries for this position
        for (List<SupplyEntry> entries : supplyTable.values()) {
            entries.removeIf(e -> e.pos.equals(pos));
        }
        supplyTable.entrySet().removeIf(e -> e.getValue().isEmpty());

        // Add fresh entries, maintaining sorted order by priority
        for (Map.Entry<IItemKey, Long> entry : items.entrySet()) {
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
     * Place a standing order with explicit fulfillment mode. Increments orderedForRequester immediately.
     *
     * @return UUID of the new order (store for later cancellation)
     */
    public UUID placeOrder(IItemKey item, long amount, BlockPos requester, FulfillmentMode fulfillmentMode) {
        if (amount <= 0) throw new IllegalArgumentException("Order amount must be positive, got: " + amount);
        UUID id = UUID.randomUUID();
        orderQueue.put(id, new Order(id, item, amount, requester, fulfillmentMode));
        orderedForRequester
                .computeIfAbsent(requester, k -> new HashMap<>())
                .merge(item, amount, Long::sum);
        NetDbg.out("Order placed: {} | {}x {} → {} (requester)",
                id.toString().substring(0, 8), amount, item.toStack(1).getItem(), requester);
        return id;
    }

    /** Place a standing order with default {@link FulfillmentMode#PARTIAL} fulfillment. */
    public UUID placeOrder(IItemKey item, long amount, BlockPos requester) {
        return placeOrder(item, amount, requester, FulfillmentMode.PARTIAL);
    }

    /**
     * Cancel a standing order by ID. Decrements orderedForRequester.
     */
    public void cancelOrder(UUID id) {
        Order order = orderQueue.remove(id);
        if (order != null) {
            NetDbg.out("Order cancelled: {}", id.toString().substring(0, 8));
            decrementOrdered(order.requester(), order.item(), order.amount());
            reservationManager.releaseByOrder(id);
        }
        Order inTransit = inTransitOrders.remove(id);
        if (inTransit != null) {
            decrementOrdered(inTransit.requester(), inTransit.item(), inTransit.amount());
            reservationManager.releaseByOrder(id);
        }
    }

    /**
     * Cancel all standing orders for a requester and clear their orderedForRequester entries.
     * Used when a pipe is removed from the network.
     */
    public void cancelOrdersFor(BlockPos requester) {
        orderQueue.entrySet().removeIf(e -> {
            if (e.getValue().requester().equals(requester)) {
                reservationManager.releaseByOrder(e.getKey());
                return true;
            }
            return false;
        });
        inTransitOrders.entrySet().removeIf(e -> {
            if (e.getValue().requester().equals(requester)) {
                reservationManager.releaseByOrder(e.getKey());
                return true;
            }
            return false;
        });
        orderedForRequester.remove(requester);
    }

    // ===== Dispatch =====

    /**
     * Mark a provider as deferred for the rest of this tick (e.g. crafter buffer is full).
     * Deferred providers are skipped in {@link #nextDispatchable()} but remain in the supply
     * table so the item stays visible in the network UI. Call {@link #clearDeferredProviders()}
     * at the start of each tick to reset this state.
     */
    public void deferProvider(BlockPos provider) {
        deferredProviders.add(provider);
    }

    /** Reset the deferred-provider set; must be called at the start of each network tick. */
    public void clearDeferredProviders() {
        deferredProviders.clear();
    }

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
            // Skip orders whose only available provider was deferred this tick (buffer full)
            if (entries.stream().allMatch(e -> deferredProviders.contains(e.pos))) continue;
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
        // Walk every entry rather than committing to the first: a provider that is deferred this
        // tick, or whose stock is entirely reserved by earlier orders, must not hide the ones
        // behind it — including the crafter, which always sorts last.
        for (int supplyIdx = 0; supplyIdx < entries.size(); supplyIdx++) {
            SupplyEntry supply = entries.get(supplyIdx);
            if (deferredProviders.contains(supply.pos)) continue;

            DispatchCommand cmd = tryDispatchFrom(orderIt, order, entries, supplyIdx, supply);
            if (cmd != null) return cmd;
            if (!orderQueue.containsKey(order.id())) return null; // cancelled for missing ingredients
        }
        return null;
    }

    /**
     * Attempt a dispatch from one supply entry. Returns {@code null} when this entry cannot serve
     * the order, leaving the caller free to try the next one.
     */
    @Nullable
    private DispatchCommand tryDispatchFrom(
            Iterator<Map.Entry<UUID, Order>> orderIt, Order order, List<SupplyEntry> entries,
            int supplyIdx, SupplyEntry supply) {
        if (supply.available == 0) {
            // On-demand supply (crafter): validate ingredient chain before dispatching
            List<IItemKey> missing = getMissingIngredients(order.item(), order.amount());
            if (!missing.isEmpty()) {
                cancelAndNotify(orderIt, order, missing);
                return null;
            }
            return new DispatchCommand(
                    order.id(), supply.pos, order.requester(), order.item(), order.amount());
        }

        // Real stock: compute effective availability (raw minus active reservations)
        long effective = reservationManager.effectiveAvailable(supply.pos, order.item(), supply.available);

        // Fully reserved by other in-flight orders; the caller moves on to the next entry.
        if (effective == 0) return null;

        if (effective >= order.amount()) {
            // Full fill from real stock: create a hard reservation
            reservationManager.reserve(order.id(), supply.pos, order.requester(),
                    order.item(), order.amount(), true);
            if (effective - order.amount() == 0) {
                entries.remove(supplyIdx);
                if (entries.isEmpty()) supplyTable.remove(order.item());
            }
            return new DispatchCommand(
                    order.id(), supply.pos, order.requester(), order.item(), order.amount());
        }

        // Partial fill from real stock — pre-check only when a crafter also covers this item
        // (the crafter will be needed to fill the remainder on a subsequent tick)
        boolean crafterExists = entries.stream().anyMatch(e -> e.available == 0);

        // FULL mode: block partial dispatch only when total available supply is insufficient.
        // getAvailableAmount sums real stock across all providers (returns Long.MAX_VALUE when
        // a crafter covers the item), so combined providers or a crafter can together satisfy
        // the order. !crafterExists alone was too narrow: it would stall when stock is split
        // across multiple providers even though their combined total is sufficient.
        if (order.fulfillmentMode() == FulfillmentMode.FULL
                && getAvailableAmount(order.item()) < order.amount()) return null;
        if (crafterExists) {
            List<IItemKey> missing = getMissingIngredients(order.item(), order.amount());
            if (!missing.isEmpty()) {
                cancelAndNotify(orderIt, order, missing);
                return null;
            }
        }

        // Pre-check passed (or no crafter covers this item): dispatch partial stock now
        reservationManager.reserve(order.id(), supply.pos, order.requester(),
                order.item(), effective, true);
        entries.remove(supplyIdx);
        if (entries.isEmpty()) supplyTable.remove(order.item());
        return new DispatchCommand(
                order.id(), supply.pos, order.requester(), order.item(), effective);
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
        reservationManager.transitionByOrder(orderId, AllocationState.IN_TRANSIT);
        trackInTransit(order, shipped);
        if (shipped >= order.amount()) {
            orderQueue.remove(orderId);
        } else {
            orderQueue.put(orderId, new Order(orderId, order.item(), order.amount() - shipped,
                    order.requester(), order.fulfillmentMode()));
        }
    }

    /**
     * Release the reservations backing an order that was reserved but never shipped, so the
     * provider's stock is free again when the order is retried.
     */
    public void releaseReservations(UUID orderId) {
        reservationManager.releaseByOrder(orderId);
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
        reservationManager.invalidateByProvider(provider);
    }

    /**
     * Record physical delivery of items to a requester. Decrements orderedForRequester.
     * Called by PipeRuntime when a TravelingItem enters an inventory.
     */
    public void notifyDelivery(BlockPos requester, IItemKey item, long amount) {
        if (amount <= 0) return;
        NetDbg.out("Delivery notified: {} received {}x {}", requester, amount, item.toStack(1).getItem());
        decrementOrdered(requester, item, amount);
        reservationManager.releaseInFlight(requester, item, amount);
    }

    /**
     * Record physical delivery for a tracked dispatch.
     */
    public void notifyDelivery(UUID orderId, BlockPos requester, IItemKey item, long amount) {
        if (amount <= 0) return;
        NetDbg.out("Delivery notified: {} received {}x {}", requester, amount, item.toStack(1).getItem());
        releaseInTransit(orderId, amount);
        decrementOrdered(requester, item, amount);
        // Released against the order, not the requester: two concurrent orders for the same item to
        // the same requester would otherwise resolve whichever the map iterated over first.
        reservationManager.releaseInFlight(orderId, item, amount);
    }

    /**
     * Record failed delivery for a tracked dispatch and requeue the missing amount.
     *
     * @return replacement order id, or {@code null} when the amount is not positive
     */
    @Nullable
    public UUID notifyDeliveryFailed(UUID orderId, BlockPos requester, IItemKey item, long amount) {
        if (amount <= 0) return null;

        Order tracked = releaseInTransit(orderId, amount);
        if (tracked == null) return null;

        NetDbg.out("Delivery failed: {} lost {}x {}",
                tracked.requester(), tracked.amount(), tracked.item().toStack(1).getItem());
        decrementOrdered(tracked.requester(), tracked.item(), tracked.amount());
        // The provider never lost these items, so its reservation has to go back with them.
        reservationManager.releaseInFlight(orderId, tracked.item(), tracked.amount());

        return placeOrder(tracked.item(), tracked.amount(), tracked.requester(), tracked.fulfillmentMode());
    }

    /**
     * Record failed delivery for a dispatch without an id. This only releases requester
     * accounting because there is no reliable in-flight order to retry or reservation to match.
     */
    public void notifyDeliveryFailedNoId(BlockPos requester, IItemKey item, long amount) {
        if (amount <= 0) return;
        NetDbg.out("Untracked delivery failed: {} lost {}x {}", requester, amount, item.toStack(1).getItem());
        decrementOrdered(requester, item, amount);
    }

    // ===== Ingredient Chain Validation =====

    /**
     * Dry-run check: returns empty list if the network can satisfy {@code amount} of {@code item}
     * (from real stock + dynamic providers, recursively). Otherwise returns the terminal missing
     * ingredient(s). Creates a fresh shared-reservation context for the whole validation tree.
     */
    public List<IItemKey> getMissingIngredients(IItemKey item, long amount) {
        Map<IItemKey, Long> claimed = new HashMap<>();
        Set<IItemKey> visited = new HashSet<>();
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
    private List<IItemKey> collectMissing(
            IItemKey item, long amount,
            Map<IItemKey, Long> claimed, Set<IItemKey> visited) {
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
                    if (check == null) {
                        // Provider registered supply with available=0 but never registered a
                        // ProviderCanFulfill check — treat as unfulfillable so we don't silently
                        // dispatch to a provider that cannot prove it can satisfy the request.
                        LogisticsMod.LOGGER.warn("No ProviderCanFulfill registered for provider at {} — "
                                + "treating as missing (missing registerProviderCheck call?)", e.pos);
                        return List.of(item);
                    }
                    IngredientChecker checker = (ing, amt) ->
                            collectMissing(ItemStorageLookup.of(ing), amt, claimed, visited);
                    List<IItemKey> missing = check.getMissing(remaining, checker);
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

    // ===== Crafter Snapshot Registration =====

    public void registerCrafterSnapshot(BlockPos pos, CrafterSnapshot snapshot) {
        crafterSnapshots.put(pos, snapshot);
    }

    public void unregisterCrafterSnapshot(BlockPos pos) {
        crafterSnapshots.remove(pos);
    }

    // ===== Order Query =====

    /** {@code true} if the order is still pending in the queue (not yet fully dispatched). */
    public boolean hasOrder(UUID orderId) {
        return orderQueue.containsKey(orderId);
    }

    // ===== PlanningView =====

    /**
     * Return supply entries for an item as immutable {@link PlanningView.SupplyPoint} records.
     * Entries are in priority order (same as the internal supply table).
     */
    @Override
    public List<PlanningView.SupplyPoint> getSupply(IItemKey item) {
        List<SupplyEntry> entries = supplyTable.get(item);
        if (entries == null || entries.isEmpty()) return List.of();
        List<PlanningView.SupplyPoint> result = new ArrayList<>(entries.size());
        for (SupplyEntry e : entries) {
            result.add(new PlanningView.SupplyPoint(e.pos, e.available, e.priority));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Return the registered {@link ProviderCanFulfill} check for a dynamic provider, or
     * {@code null} if none is registered.
     */
    @Override
    @Nullable
    public ProviderCanFulfill getCrafterCheck(BlockPos provider) {
        return providerChecks.get(provider);
    }

    /**
     * Return the most recent {@link CrafterSnapshot} for a crafter position, or {@code null}
     * if none has been registered yet.
     */
    @Override
    @Nullable
    public CrafterSnapshot getCrafterSnapshot(BlockPos provider) {
        return crafterSnapshots.get(provider);
    }

    // ===== Query =====

    /**
     * Get how many items are currently ordered (in-flight or pending) for a requester.
     * Used by suppliers/requesters to avoid placing duplicate orders.
     */
    public long getOrderedAmountFor(BlockPos requester, IItemKey item) {
        Map<IItemKey, Long> map = orderedForRequester.get(requester);
        return map == null ? 0L : map.getOrDefault(item, 0L);
    }

    /**
     * Get total available supply for an item across all providers.
     * Returns Long.MAX_VALUE if any crafter entry (available == 0) can produce it on demand,
     * since the item can always be crafted. Otherwise returns the sum of real-stock amounts.
     */
    public long getAvailableAmount(IItemKey item) {
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
        for (Map.Entry<IItemKey, List<SupplyEntry>> entry : supplyTable.entrySet()) {
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
        for (Map.Entry<IItemKey, List<SupplyEntry>> entry : other.supplyTable.entrySet()) {
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

        // Merge in-transit orders (preserve local entries on UUID collision)
        for (Map.Entry<UUID, Order> entry : other.inTransitOrders.entrySet()) {
            inTransitOrders.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Merge orderedForRequester
        for (Map.Entry<BlockPos, Map<IItemKey, Long>> entry : other.orderedForRequester.entrySet()) {
            Map<IItemKey, Long> myMap =
                    orderedForRequester.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            entry.getValue().forEach((variant, amount) -> myMap.merge(variant, amount, Long::sum));
        }

        // Merge provider checks
        providerChecks.putAll(other.providerChecks);

        // Merge reservations
        reservationManager.merge(other.reservationManager);

        // Merge crafter snapshots
        crafterSnapshots.putAll(other.crafterSnapshots);
    }

    // ===== Helpers =====

    private void cancelAndNotify(
            Iterator<Map.Entry<UUID, Order>> orderIt, Order order, List<IItemKey> missing) {
        orderIt.remove();
        decrementOrdered(order.requester(), order.item(), order.amount());
        reservationManager.releaseByOrder(order.id());
        NetDbg.out("Order failed (missing ingredients): {} | {}x {} → {} | missing: {}",
                order.id().toString().substring(0, 8), order.amount(),
                order.item().toStack(1).getItem(), order.requester(), missing);
        if (failureListener != null) {
            failureListener.onOrderFailed(
                    order.id(), order.requester(), order.item(), order.amount(), missing);
        }
    }

    private void decrementOrdered(BlockPos requester, IItemKey item, long amount) {
        Map<IItemKey, Long> map = orderedForRequester.get(requester);
        if (map != null) {
            long remaining = map.merge(item, -amount, Long::sum);
            if (remaining <= 0) map.remove(item);
            if (map.isEmpty()) orderedForRequester.remove(requester);
        }
    }

    private void trackInTransit(Order order, long shipped) {
        if (shipped <= 0) return;
        inTransitOrders.merge(
                order.id(),
                new Order(order.id(), order.item(), shipped, order.requester(), order.fulfillmentMode()),
                (existing, added) -> new Order(
                        existing.id(),
                        existing.item(),
                        existing.amount() + added.amount(),
                        existing.requester(),
                        existing.fulfillmentMode()));
    }

    @Nullable
    private Order releaseInTransit(UUID orderId, long amount) {
        Order tracked = inTransitOrders.get(orderId);
        if (tracked == null || amount <= 0) return tracked;

        if (amount >= tracked.amount()) {
            inTransitOrders.remove(orderId);
            return tracked;
        }

        inTransitOrders.put(orderId, new Order(
                tracked.id(),
                tracked.item(),
                tracked.amount() - amount,
                tracked.requester(),
                tracked.fulfillmentMode()));
        return new Order(tracked.id(), tracked.item(), amount, tracked.requester(), tracked.fulfillmentMode());
    }
}
