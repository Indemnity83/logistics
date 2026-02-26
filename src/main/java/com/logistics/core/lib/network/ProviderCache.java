package com.logistics.core.lib.network;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Cached inventory contents for a Provider pipe.
 * Updated periodically (every 1 second) to avoid constant inventory scanning.
 */
public class ProviderCache {
    private Map<ItemStack, Long> availableItems = new HashMap<>();
    private long lastUpdate;

    public void update(Map<ItemStack, Long> items, long gameTime) {
        this.availableItems = new HashMap<>(items);
        this.lastUpdate = gameTime;
    }

    public long getAvailableAmount(ItemStack stack) {
        for (Map.Entry<ItemStack, Long> entry : availableItems.entrySet()) {
            if (ItemStack.isSameItemSameComponents(entry.getKey(), stack)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public boolean isStale(long currentTime, int maxAge) {
        return currentTime - lastUpdate > maxAge;
    }

    public Map<ItemStack, Long> getAvailableItems() {
        return new HashMap<>(availableItems);
    }

    public long getLastUpdate() {
        return lastUpdate;
    }
}
