package com.logistics.pipe.network;

import com.logistics.core.lib.network.CrafterSnapshot;
import net.minecraft.core.BlockPos;

import java.util.Map;

/**
 * Immutable point-in-time view of the full logistics network state.
 * Aggregates snapshots of all providers, suppliers, and crafters in the network.
 *
 * <p>Built by {@link NetworkSnapshotBuilder}. Used by planning and reconciliation
 * services to reason about the network without querying live Minecraft world state.
 *
 * <p>Pure value object — no Minecraft world coupling beyond BlockPos.
 */
public record NetworkSnapshot(
        Map<BlockPos, ProviderSnapshot> providers,
        Map<BlockPos, SupplierSnapshot> suppliers,
        Map<BlockPos, CrafterSnapshot> crafters) {

    public NetworkSnapshot {
        providers = Map.copyOf(providers);
        suppliers = Map.copyOf(suppliers);
        crafters = Map.copyOf(crafters);
    }

    /** An empty snapshot — no nodes registered. */
    public static final NetworkSnapshot EMPTY = new NetworkSnapshot(Map.of(), Map.of(), Map.of());
}
