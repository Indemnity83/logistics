package com.logistics.pipe.network;

import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * A node in a {@link FulfillmentPlan} tree describing how to source an item.
 *
 * <p>Sealed hierarchy:
 * <ul>
 *   <li>{@link ExtractNode} — pull items from a real-stock provider</li>
 *   <li>{@link CraftNode}   — trigger a crafter to produce items</li>
 * </ul>
 */
public sealed interface PlanNode permits PlanNode.ExtractNode, PlanNode.CraftNode {

    record ExtractNode(
            BlockPos provider,
            IItemKey item,
            long amount) implements PlanNode {}

    record CraftNode(
            BlockPos crafter,
            IItemKey output,
            long batches,
            List<PlanNode> ingredientDeps) implements PlanNode {}
}
