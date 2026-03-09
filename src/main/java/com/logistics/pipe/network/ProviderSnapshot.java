package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.Map;

/**
 * Immutable snapshot of what a provider pipe has available at the moment of capture.
 * Pure value object — no Minecraft world coupling.
 */
public record ProviderSnapshot(BlockPos pos, Map<ItemVariant, Long> stock, int priority) {
    public ProviderSnapshot {
        if (pos == null) throw new NullPointerException("pos must not be null");
        stock = Map.copyOf(stock); // defensive immutable copy
    }

    /** Total available amount of a specific item in this snapshot. */
    public long available(ItemVariant item) {
        return stock.getOrDefault(item, 0L);
    }
}
