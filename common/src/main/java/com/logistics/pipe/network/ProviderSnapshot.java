package com.logistics.pipe.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.Map;

/**
 * Immutable snapshot of what a provider pipe has available at the moment of capture.
 * Pure value object — no Minecraft world coupling.
 */
public record ProviderSnapshot(BlockPos pos, Map<IItemKey, Long> stock, int priority) {
    public ProviderSnapshot {
        if (pos == null) throw new NullPointerException("pos must not be null");
        stock = Map.copyOf(stock);
    }

    public long available(IItemKey item) {
        return stock.getOrDefault(item, 0L);
    }
}
