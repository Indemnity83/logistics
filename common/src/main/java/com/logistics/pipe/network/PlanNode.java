package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * A node in a {@link FulfillmentPlan} tree describing how to source an item.
 *
 * <p>Sealed hierarchy — exactly two concrete forms:
 * <ul>
 *   <li>{@link ExtractNode} — pull items from a real-stock provider</li>
 *   <li>{@link CraftNode}   — trigger a crafter to produce items</li>
 * </ul>
 */
public sealed interface PlanNode permits PlanNode.ExtractNode, PlanNode.CraftNode {

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Pull {@code amount} of {@code item} from a real-stock provider.
     */
    record ExtractNode(
            BlockPos provider,
            ItemVariant item,
            long amount) implements PlanNode {}

    // ───────────────────────────────────────────────────────────────────────

    /**
     * Trigger a crafter to produce {@code batches} worth of {@code output}.
     *
     * <p>{@code ingredientDeps} lists sub-plans for ingredients that must be sourced first.
     * Empty in Phase 6 (ingredient recursion is added in a later phase).
     */
    record CraftNode(
            BlockPos crafter,
            ItemVariant output,
            long batches,
            List<PlanNode> ingredientDeps) implements PlanNode {}
}
