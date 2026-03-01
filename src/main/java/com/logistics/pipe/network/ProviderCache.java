package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Cached inventory contents for a Provider pipe.
 * Updated periodically (every 1 second) to avoid constant inventory scanning.
 */
public class ProviderCache {
    private Map<ItemVariant, Long> availableItems = new HashMap<>();
    private long lastUpdate;

    public void update(Map<ItemStack, Long> items, long gameTime) {
        Map<ItemVariant, Long> converted = new HashMap<>(items.size());
        for (Map.Entry<ItemStack, Long> entry : items.entrySet()) {
            converted.merge(ItemVariant.of(entry.getKey()), entry.getValue(), Long::sum);
        }
        this.availableItems = converted;
        this.lastUpdate = gameTime;
    }

    public long getAvailableAmount(ItemStack stack) {
        return availableItems.getOrDefault(ItemVariant.of(stack), 0L);
    }

    public boolean isStale(long currentTime, int maxAge) {
        return currentTime - lastUpdate > maxAge;
    }

    /** Returns an unmodifiable view of the internal variant map — no copies. */
    Map<ItemVariant, Long> getAvailableVariants() {
        return Collections.unmodifiableMap(availableItems);
    }

    public Map<ItemStack, Long> getAvailableItems() {
        Map<ItemStack, Long> result = new HashMap<>(availableItems.size());
        for (Map.Entry<ItemVariant, Long> entry : availableItems.entrySet()) {
            result.put(entry.getKey().toStack(1), entry.getValue());
        }
        return result;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
