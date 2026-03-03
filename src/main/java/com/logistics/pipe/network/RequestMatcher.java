package com.logistics.pipe.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.network.LogisticsOrder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Pure request matching logic.
 * Handles provider caching, request queuing, and order creation.
 * Zero Minecraft API coupling - 100% testable with pure Java.
 */
public class RequestMatcher {
    // Skip provider caches that haven't been updated in 3 scan intervals (60 ticks = 3 seconds).
    // Providers update every 20 ticks; if a provider misses 3 updates it is considered offline.
    private static final int CACHE_MAX_AGE_TICKS = 60;

    // In-transit orders whose TravelingItem has gone missing (e.g. chunk reload) are cleaned up
    // after this many ticks so orderedForRequester doesn't drift forever.
    private static final int IN_TRANSIT_CLEANUP_TTL = 1200; // 60 seconds

    private final Map<BlockPos, ProviderCache> providerCaches = new HashMap<>();
    private final Map<BlockPos, ProviderCache> crafterCaches = new HashMap<>();
    private final List<ItemRequest> pendingRequests = new ArrayList<>();
    private final Map<BlockPos, List<LogisticsOrder>> pendingOrders = new HashMap<>();
    private final Map<BlockPos, Map<ItemVariant, Long>> orderedForRequester = new HashMap<>();
    private final List<LogisticsOrder> inTransitOrders = new ArrayList<>();

    /**
     * Update provider cache for a specific position.
     */
    public void updateProviderCache(BlockPos pos, Map<ItemStack, Long> items, long gameTime) {
        providerCaches.computeIfAbsent(pos, k -> new ProviderCache()).update(items, gameTime);
    }

    /**
     * Remove provider cache for a position (when pipe is removed).
     */
    public void removeProviderCache(BlockPos pos) {
        providerCaches.remove(pos);
    }

    /**
     * Update crafter cache for a crafting pipe position.
     */
    public void updateCrafterCache(BlockPos pos, Map<ItemStack, Long> items, long gameTime) {
        crafterCaches.computeIfAbsent(pos, k -> new ProviderCache()).update(items, gameTime);
    }

    /**
     * Remove crafter cache for a position (when crafting pipe is removed).
     */
    public void removeCrafterCache(BlockPos pos) {
        crafterCaches.remove(pos);
    }

    /**
     * Check if a position has an active crafter cache.
     */
    public boolean isCraftingProvider(BlockPos pos) {
        return crafterCaches.containsKey(pos);
    }

    /**
     * Get available amount of an item across all providers and crafters.
     */
    public long getAvailableAmount(ItemStack stack) {
        long total = 0;
        for (ProviderCache cache : providerCaches.values()) {
            total += cache.getAvailableAmount(stack);
        }
        for (ProviderCache cache : crafterCaches.values()) {
            long crafterAmount = cache.getAvailableAmount(stack);
            // Crafter caches advertise Long.MAX_VALUE; cap to avoid overflow
            if (crafterAmount > 0) {
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    /**
     * Get all available items from all providers.
     * Returns a map of ItemStack to total available amount.
     */
    public Map<ItemStack, Long> getAllAvailableItems() {
        Map<ItemVariant, Long> aggregated = new HashMap<>();

        for (ProviderCache cache : providerCaches.values()) {
            cache.getAvailableVariants().forEach((variant, amount) ->
                    aggregated.merge(variant, amount, Long::sum));
        }

        for (ProviderCache cache : crafterCaches.values()) {
            cache.getAvailableVariants().forEach((variant, amount) -> {
                if (amount > 0) {
                    aggregated.merge(variant, Long.MAX_VALUE, (a, b) -> Long.MAX_VALUE);
                }
            });
        }

        Map<ItemStack, Long> result = new HashMap<>(aggregated.size());
        for (Map.Entry<ItemVariant, Long> entry : aggregated.entrySet()) {
            result.put(entry.getKey().toStack(1), entry.getValue());
        }
        return result;
    }

    /**
     * Find provider position that has the requested item.
     * Skips stale caches (providers that have not updated recently).
     * Checks crafterCaches when providerCaches yields nothing.
     * Returns null if no active provider has sufficient quantity.
     */
    @Nullable
    BlockPos findProviderFor(ItemStack stack, long amount, long gameTime) {
        for (Map.Entry<BlockPos, ProviderCache> entry : providerCaches.entrySet()) {
            if (entry.getValue().isStale(gameTime, CACHE_MAX_AGE_TICKS)) {
                continue;
            }
            if (entry.getValue().getAvailableAmount(stack) >= amount) {
                return entry.getKey();
            }
        }
        // Fall back to crafter caches
        for (Map.Entry<BlockPos, ProviderCache> entry : crafterCaches.entrySet()) {
            if (entry.getValue().isStale(gameTime, CACHE_MAX_AGE_TICKS)) {
                continue;
            }
            if (entry.getValue().getAvailableAmount(stack) > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Add a request to the queue.
     */
    public void addRequest(ItemRequest request) {
        pendingRequests.add(request);
    }

    /**
     * Get all pending requests (for testing/debugging).
     */
    public List<ItemRequest> getPendingRequests() {
        return Collections.unmodifiableList(pendingRequests);
    }

    /**
     * Add an order for a specific provider.
     */
    public void addOrder(LogisticsOrder order) {
        pendingOrders.computeIfAbsent(order.provider(), k -> new ArrayList<>()).add(order);
        orderedForRequester
            .computeIfAbsent(order.requester(), k -> new HashMap<>())
            .merge(ItemVariant.of(order.stack()), order.amount(), Long::sum);
    }

    /**
     * Get the total amount of an item currently ordered for a requester.
     * Returns 0 once the provider has shipped the items (order removed).
     */
    public long getOrderedAmountFor(BlockPos requester, ItemVariant variant) {
        Map<ItemVariant, Long> map = orderedForRequester.get(requester);
        return map == null ? 0L : map.getOrDefault(variant, 0L);
    }

    /**
     * Get pending orders for a specific provider.
     */
    public List<LogisticsOrder> getOrdersFor(BlockPos provider) {
        return pendingOrders.getOrDefault(provider, Collections.emptyList());
    }

    /**
     * Remove a completed order.
     */
    public void removeOrder(LogisticsOrder order) {
        List<LogisticsOrder> orders = pendingOrders.get(order.provider());
        if (orders != null) {
            orders.remove(order);
        }
        Map<ItemVariant, Long> requesterMap = orderedForRequester.get(order.requester());
        if (requesterMap != null) {
            ItemVariant variant = ItemVariant.of(order.stack());
            long remaining = requesterMap.merge(variant, -order.amount(), Long::sum);
            if (remaining <= 0) requesterMap.remove(variant);
        }
    }

    /**
     * Remove all orders for a position (when pipe is removed as provider).
     */
    public void removeOrdersFor(BlockPos providerPos) {
        List<LogisticsOrder> removed = pendingOrders.remove(providerPos);
        if (removed != null) {
            for (LogisticsOrder order : removed) {
                Map<ItemVariant, Long> requesterMap = orderedForRequester.get(order.requester());
                if (requesterMap != null) {
                    ItemVariant variant = ItemVariant.of(order.stack());
                    long remaining = requesterMap.merge(variant, -order.amount(), Long::sum);
                    if (remaining <= 0) requesterMap.remove(variant);
                }
            }
        }
    }

    /**
     * Transition an order from pending to in-transit when a provider ships items.
     * Removes the pending order, requeues any unshipped remainder, and creates an
     * in-transit record that keeps orderedForRequester positive until delivery.
     * Does NOT change orderedForRequester — that only decrements on confirmDelivery.
     *
     * @param pendingOrder The order that was pending at the provider
     * @param shippedAmount Actual amount extracted (≤ pendingOrder.amount())
     * @return The in-transit order to attach to the TravelingItem
     */
    public LogisticsOrder markShipped(LogisticsOrder pendingOrder, long shippedAmount, long gameTime) {
        // Remove from pending (without touching orderedForRequester)
        List<LogisticsOrder> orders = pendingOrders.get(pendingOrder.provider());
        if (orders != null) orders.remove(pendingOrder);

        // Requeue remainder using current gameTime so the remainder doesn't inherit a stale
        // createdAt and get cleaned up prematurely by cleanupStaleInTransit.
        long remainder = pendingOrder.amount() - shippedAmount;
        if (remainder > 0) {
            LogisticsOrder remainderOrder = new LogisticsOrder(
                pendingOrder.provider(), pendingOrder.requester(), pendingOrder.stack(),
                remainder, gameTime);
            pendingOrders.computeIfAbsent(pendingOrder.provider(), k -> new ArrayList<>()).add(remainderOrder);
        }

        // Create in-transit record with current gameTime for accurate staleness tracking
        LogisticsOrder inTransitOrder = new LogisticsOrder(
            pendingOrder.provider(), pendingOrder.requester(), pendingOrder.stack(),
            shippedAmount, gameTime);
        inTransitOrders.add(inTransitOrder);
        return inTransitOrder;
    }

    /**
     * Confirm that an in-transit item was physically delivered to an inventory.
     * Decrements orderedForRequester so the supplier knows stock has arrived.
     */
    public void confirmDelivery(LogisticsOrder inTransitOrder) {
        boolean removed = inTransitOrders.remove(inTransitOrder);
        if (removed) {
            Map<ItemVariant, Long> requesterMap = orderedForRequester.get(inTransitOrder.requester());
            if (requesterMap != null) {
                ItemVariant variant = ItemVariant.of(inTransitOrder.stack());
                long remaining = requesterMap.merge(variant, -inTransitOrder.amount(), Long::sum);
                if (remaining <= 0) requesterMap.remove(variant);
            }
        }
    }

    /**
     * Clean up in-transit orders whose TravelingItem has gone missing (e.g. chunk reload).
     * Uses the order's createdAt timestamp; orders older than IN_TRANSIT_CLEANUP_TTL are removed
     * and their amounts are removed from orderedForRequester.
     */
    public void cleanupStaleInTransit(long gameTime) {
        Iterator<LogisticsOrder> it = inTransitOrders.iterator();
        while (it.hasNext()) {
            LogisticsOrder order = it.next();
            if (gameTime - order.createdAt() > IN_TRANSIT_CLEANUP_TTL) {
                it.remove();
                Map<ItemVariant, Long> requesterMap = orderedForRequester.get(order.requester());
                if (requesterMap != null) {
                    ItemVariant variant = ItemVariant.of(order.stack());
                    long remaining = requesterMap.merge(variant, -order.amount(), Long::sum);
                    if (remaining <= 0) requesterMap.remove(variant);
                }
            }
        }
    }

    /**
     * Process pending requests and create orders.
     * Matches requests to providers and creates orders.
     *
     * @param gameTime Current game time for order timestamps
     * @return Number of requests successfully matched
     */
    public int processRequests(long gameTime) {
        int matchedCount = 0;
        Iterator<ItemRequest> iterator = pendingRequests.iterator();

        while (iterator.hasNext()) {
            ItemRequest request = iterator.next();
            int maxStackSize = request.stack().getMaxStackSize();
            long remaining = request.amount();
            List<LogisticsOrder> orders = new ArrayList<>();

            while (remaining > 0) {
                long chunkSize = Math.min(remaining, maxStackSize);
                BlockPos provider = findProviderFor(request.stack(), chunkSize, gameTime);
                if (provider == null) break;
                orders.add(new LogisticsOrder(provider, request.requester(), request.stack(), chunkSize, gameTime));
                remaining -= chunkSize;
            }

            if (remaining == 0) {
                orders.forEach(this::addOrder);
                iterator.remove();
                matchedCount++;
            }
        }

        return matchedCount;
    }

    /**
     * Merge another RequestMatcher into this one.
     * Used when merging networks.
     */
    public void merge(RequestMatcher other) {
        providerCaches.putAll(other.providerCaches);
        crafterCaches.putAll(other.crafterCaches);
        pendingRequests.addAll(other.pendingRequests);
        inTransitOrders.addAll(other.inTransitOrders);

        for (Map.Entry<BlockPos, List<LogisticsOrder>> entry : other.pendingOrders.entrySet()) {
            pendingOrders.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }

        for (Map.Entry<BlockPos, Map<ItemVariant, Long>> entry : other.orderedForRequester.entrySet()) {
            Map<ItemVariant, Long> thisMap =
                orderedForRequester.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            entry.getValue().forEach((variant, amount) -> thisMap.merge(variant, amount, Long::sum));
        }
    }
}
