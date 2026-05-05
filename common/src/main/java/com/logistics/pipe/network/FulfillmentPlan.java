package com.logistics.pipe.network;

import java.util.List;

/**
 * Result of {@link RequestPlanner#plan}: describes how the network intends to source an item.
 *
 * @param roots         top-level plan nodes (extract / craft steps), in execution order
 * @param plannedAmount total amount the plan commits to delivering (may be less than requested
 *                      for PARTIAL plans)
 * @param full          {@code true} if the plan satisfies the entire requested amount
 */
public record FulfillmentPlan(List<PlanNode> roots, long plannedAmount, boolean full) {

    /** Empty plan — returned when the request cannot be satisfied at all. */
    public static FulfillmentPlan empty() {
        return new FulfillmentPlan(List.of(), 0L, false);
    }

    /** {@code true} if no supply was found (no plan nodes). */
    public boolean isEmpty() {
        return roots.isEmpty();
    }
}
