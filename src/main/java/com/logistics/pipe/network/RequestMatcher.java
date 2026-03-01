package com.logistics.pipe.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    private final Map<BlockPos, ProviderCache> providerCaches = new HashMap<>();
    private final List<ItemRequest> pendingRequests = new ArrayList<>();
    private final Map<BlockPos, List<LogisticsOrder>> pendingOrders = new HashMap<>();

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
     * Get available amount of an item across all providers.
     */
    public long getAvailableAmount(ItemStack stack) {
        long total = 0;
        for (ProviderCache cache : providerCaches.values()) {
            total += cache.getAvailableAmount(stack);
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
            for (Map.Entry<ItemStack, Long> entry : cache.getAvailableItems().entrySet()) {
                aggregated.merge(ItemVariant.of(entry.getKey()), entry.getValue(), Long::sum);
            }
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
    }

    /**
     * Remove all orders for a position (when pipe is removed).
     */
    public void removeOrdersFor(BlockPos pos) {
        pendingOrders.remove(pos);
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
        pendingRequests.addAll(other.pendingRequests);

        for (Map.Entry<BlockPos, List<LogisticsOrder>> entry : other.pendingOrders.entrySet()) {
            pendingOrders.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
        }
    }
}
