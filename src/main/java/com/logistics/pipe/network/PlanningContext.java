package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scratch-pad used by {@link RequestPlanner} during a single planning run.
 *
 * <p>{@code claimed} mirrors the shared-reservation pattern in
 * {@link NetworkController#collectMissing}: as each plan branch "uses" stock, the amounts are
 * deducted from effective availability so sibling branches that need the same material see
 * reduced stock. It is never written back to the real supply table.
 *
 * <p>{@code visited} is a DFS path guard against ingredient cycles.  Items are removed on exit
 * from each recursive frame so they can appear in separate branches of the tree.
 */
final class PlanningContext {

    final Map<ItemVariant, Long> claimed = new HashMap<>();
    final Set<ItemVariant> visited = new HashSet<>();

    static PlanningContext fresh() {
        return new PlanningContext();
    }

    /**
     * Effective real stock for {@code item} from the given supply entries, minus amounts already
     * claimed in this planning run.
     * Crafter entries ({@code available == 0}) are excluded from the stock sum.
     */
    long effectiveRealStock(ItemVariant item, List<PlanningView.SupplyPoint> supply) {
        long raw = 0;
        for (PlanningView.SupplyPoint sp : supply) {
            if (sp.available() > 0) raw += sp.available();
        }
        return Math.max(0L, raw - claimed.getOrDefault(item, 0L));
    }
}
