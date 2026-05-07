package com.logistics.pipe.network;

import com.logistics.core.lib.network.*;
import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates snapshot data registered by modules and builds a {@link NetworkSnapshot}.
 *
 * <p>A new builder should be created each snapshot cycle; builders are not thread-safe
 * and are not reused across ticks.
 */
public class NetworkSnapshotBuilder {

    private final Map<BlockPos, ProviderSnapshot> providers = new HashMap<>();
    private final Map<BlockPos, SupplierSnapshot> suppliers = new HashMap<>();
    private final Map<BlockPos, CrafterSnapshot> crafters = new HashMap<>();

    public void registerProvider(BlockPos pos, Map<IItemKey, Long> stock, int priority) {
        providers.put(pos, new ProviderSnapshot(pos, stock, priority));
    }

    public void registerSupplier(BlockPos pos, List<SupplySlotState> slots) {
        suppliers.put(pos, new SupplierSnapshot(pos, slots));
    }

    public void registerCrafter(BlockPos pos, IItemKey output, long outputCount,
                                List<RecipeIngredient> ingredients, CrafterBufferState buffer) {
        crafters.put(pos, new CrafterSnapshot(pos, output, outputCount, ingredients, buffer));
    }

    public void unregister(BlockPos pos) {
        providers.remove(pos);
        suppliers.remove(pos);
        crafters.remove(pos);
    }

    public NetworkSnapshot build() {
        return new NetworkSnapshot(providers, suppliers, crafters);
    }
}
