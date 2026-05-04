package com.logistics.pipe.network;

import com.logistics.core.lib.network.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates snapshot data registered by modules and builds a {@link NetworkSnapshot}.
 *
 * <p>Modules call the {@code register*} methods (analogous to
 * {@link ILogisticsNetwork#registerSupply}) to contribute their current state.
 * Once all modules have reported, {@link #build()} produces an immutable snapshot.
 *
 * <p>A new builder should be created each snapshot cycle; builders are not thread-safe
 * and are not reused across ticks.
 */
public class NetworkSnapshotBuilder {

    private final Map<BlockPos, ProviderSnapshot> providers = new HashMap<>();
    private final Map<BlockPos, SupplierSnapshot> suppliers = new HashMap<>();
    private final Map<BlockPos, CrafterSnapshot> crafters = new HashMap<>();

    /**
     * Register or replace the provider snapshot for a position.
     *
     * @param pos      provider pipe position
     * @param stock    current available items
     * @param priority dispatch priority (1 = real stock, 5 = crafter/on-demand)
     */
    public void registerProvider(BlockPos pos, Map<ItemVariant, Long> stock, int priority) {
        providers.put(pos, new ProviderSnapshot(pos, stock, priority));
    }

    /**
     * Register or replace the supplier snapshot for a position.
     *
     * @param pos   supplier pipe position
     * @param slots current state of each configured supply slot
     */
    public void registerSupplier(BlockPos pos, List<SupplySlotState> slots) {
        suppliers.put(pos, new SupplierSnapshot(pos, slots));
    }

    /**
     * Register or replace the crafter snapshot for a position.
     *
     * @param pos         crafting pipe position
     * @param output      item the crafter produces
     * @param outputCount items produced per batch
     * @param ingredients recipe ingredients
     * @param buffer      current autocrafter buffer capacity
     */
    public void registerCrafter(BlockPos pos, ItemVariant output, long outputCount,
                                List<RecipeIngredient> ingredients, CrafterBufferState buffer) {
        crafters.put(pos, new CrafterSnapshot(pos, output, outputCount, ingredients, buffer));
    }

    /** Remove all snapshot entries for a position (e.g. when a pipe is removed). */
    public void unregister(BlockPos pos) {
        providers.remove(pos);
        suppliers.remove(pos);
        crafters.remove(pos);
    }

    /** Build an immutable {@link NetworkSnapshot} from the current registered state. */
    public NetworkSnapshot build() {
        return new NetworkSnapshot(providers, suppliers, crafters);
    }
}
