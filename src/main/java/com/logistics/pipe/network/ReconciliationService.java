package com.logistics.pipe.network;

import java.util.Optional;

/**
 * Detects supply loss for active jobs and attempts replanning.
 *
 * <p>{@link #reconcile} checks whether the network can still source a job's outstanding items.
 * If supply has dropped, {@link #replan} tries to find an alternative source.
 *
 * <p>Both methods operate on a {@link PlanningView} snapshot so they are decoupled from mutable
 * controller internals and remain fully testable in isolation.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class ReconciliationService {

    private final RequestPlanner planner = new RequestPlanner();

    /**
     * Check whether the network can still source the job's outstanding items from current supply.
     *
     * <p>Uses {@link RequestPlanner} in PARTIAL mode to measure how much of the outstanding
     * amount is currently available. The difference is the amount lost.
     *
     * <p>Should only be called when the job's underlying order is still pending in the
     * controller queue (not yet dispatched) to avoid false positives from in-transit items.
     *
     * @param job  an active job to check
     * @param view current network supply state
     * @return result with {@code amountLost > 0} if supply is insufficient
     */
    public ReconciliationResult reconcile(NetworkJob job, PlanningView view) {
        long outstanding = job.outstanding();
        if (outstanding <= 0) return ReconciliationResult.none();

        // Ask the planner how much of the outstanding amount can currently be sourced
        FulfillmentPlan plan = planner.plan(job.item(), outstanding, FulfillmentMode.PARTIAL, view);
        long lost = outstanding - plan.plannedAmount();
        return lost > 0 ? new ReconciliationResult(lost) : ReconciliationResult.none();
    }

    /**
     * Attempt to find alternative supply for {@code missingAmount} units.
     *
     * <p>Respects the job's {@link FulfillmentMode}: a FULL-mode job will only return a plan if
     * the entire missing amount can be sourced.
     *
     * @param job           the job that experienced stock loss
     * @param missingAmount how many units to replan for
     * @param view          current network supply state
     * @return the new plan, or empty if no alternative supply exists
     */
    public Optional<FulfillmentPlan> replan(NetworkJob job, long missingAmount, PlanningView view) {
        FulfillmentPlan plan = planner.plan(job.item(), missingAmount, job.fulfillmentMode(), view);
        return plan.isEmpty() ? Optional.empty() : Optional.of(plan);
    }
}
