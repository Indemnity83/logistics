package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds a {@link FulfillmentPlan} describing how the network can source a requested item.
 *
 * <p>Planning strategy (per item):
 * <ol>
 *   <li>Consume available real stock (providers with {@code available > 0}) first.</li>
 *   <li>Fall back to the first registered crafter ({@code available == 0}) for any remainder.</li>
 * </ol>
 *
 * <p>A shared {@link PlanningContext} scratch-pad mirrors the sibling-deduction pattern from
 * {@link NetworkController#collectMissing}: claimed amounts are deducted so that sibling
 * branches that need the same material see reduced stock within the same planning run.
 *
 * <p>Phase 6 scope: crafter {@code ingredientDeps} are left empty (no recursive ingredient
 * planning). Ingredient recursion and batch-size limiting are added in later phases.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class RequestPlanner {

    /**
     * Build a fulfillment plan for the given item request.
     *
     * @param item   the item to source
     * @param amount how many to source
     * @param mode   {@link FulfillmentMode#FULL} requires full satisfaction; PARTIAL allows less
     * @param view   current network supply state
     * @return a plan describing how to source the item;
     *         {@link FulfillmentPlan#empty()} if FULL mode and cannot fully satisfy
     */
    public FulfillmentPlan plan(ItemVariant item, long amount, FulfillmentMode mode, PlanningView view) {
        PlanningContext ctx = PlanningContext.fresh();
        List<PlanNode> roots = new ArrayList<>();
        long planned = planItem(item, amount, view, ctx, roots);
        boolean full = (planned >= amount);
        if (mode == FulfillmentMode.FULL && !full) return FulfillmentPlan.empty();
        return new FulfillmentPlan(Collections.unmodifiableList(roots), planned, full);
    }

    /**
     * Plan how to source {@code needed} units of {@code item}.
     * Consumes real stock first, then falls back to the first valid crafter.
     *
     * @return amount planned (may be less than needed if supply is insufficient)
     */
    private long planItem(
            ItemVariant item, long needed,
            PlanningView view, PlanningContext ctx, List<PlanNode> out) {
        if (!ctx.visited.add(item)) return 0; // cycle guard
        try {
            List<PlanningView.SupplyPoint> supply = view.getSupply(item);
            if (supply.isEmpty()) return 0;

            long planned = 0;

            // Consume real stock first
            long effective = ctx.effectiveRealStock(item, supply);
            if (effective > 0) {
                long fromStock = Math.min(effective, needed);
                ctx.claimed.merge(item, fromStock, Long::sum);
                for (PlanningView.SupplyPoint sp : supply) {
                    if (sp.available() > 0) {
                        out.add(new PlanNode.ExtractNode(sp.provider(), item, fromStock));
                        break;
                    }
                }
                planned += fromStock;
            }

            // Try crafters for any remainder
            long remaining = needed - planned;
            if (remaining > 0) {
                CraftBatchingService batcher = new CraftBatchingService();
                for (PlanningView.SupplyPoint sp : supply) {
                    if (sp.available() != 0) continue; // skip real-stock entries
                    if (view.getCrafterCheck(sp.provider()) == null) continue; // no check registered
                    CrafterSnapshot snapshot = view.getCrafterSnapshot(sp.provider());
                    if (snapshot != null) {
                        // Cap to what the crafter's buffer can currently absorb
                        long batches = batcher.safeBatchCount(remaining, snapshot.outputCount(), snapshot.buffer());
                        if (batches <= 0) continue; // crafter buffer is full — try next
                        out.add(new PlanNode.CraftNode(sp.provider(), item, batches, List.of()));
                        planned += batches * snapshot.outputCount();
                    } else {
                        // No snapshot yet (first scan not complete): treat as unlimited capacity
                        out.add(new PlanNode.CraftNode(sp.provider(), item, remaining, List.of()));
                        planned += remaining;
                    }
                    break; // use first valid crafter
                }
            }

            return planned;
        } finally {
            ctx.visited.remove(item); // allow item to be revisited in sibling branches
        }
    }
}
