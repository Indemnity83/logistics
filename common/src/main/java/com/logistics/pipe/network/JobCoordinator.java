package com.logistics.pipe.network;

import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.network.PlanningView;
import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates {@link NetworkJob} lifecycle on top of {@link NetworkController}.
 *
 * <p>Modules submit an {@link ItemRequest} via {@link #submit}; the coordinator creates a
 * {@link NetworkJob}, places the underlying order into {@link NetworkController}, and tracks
 * delivery progress. When all items arrive the job transitions to
 * {@link JobState#COMPLETE} automatically.
 *
 * <p>{@link #tick} runs periodic reconciliation: if a job's supply disappears mid-order, the
 * coordinator attempts to replan from alternative sources or marks the job as failed.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class JobCoordinator implements NetworkController.OrderFailureListener {

    private static final int RECONCILE_INTERVAL = 100; // ~5 s at 20 tps

    private final NetworkController controller;

    // Primary store: jobId → job (insertion order = submission order)
    private final Map<UUID, NetworkJob> jobs = new LinkedHashMap<>();

    // Cross-index: orderId → jobId (for failure callbacks)
    private final Map<UUID, UUID> orderToJob = new HashMap<>();

    // Cross-index: jobId → orderId (gates reconciliation to pending orders only)
    private final Map<UUID, UUID> jobToOrder = new HashMap<>();

    private int ticksSinceReconcile = 0;

    public JobCoordinator(NetworkController controller) {
        this.controller = controller;
    }

    // ===== Job Submission =====

    /**
     * Submit a request to the network. Creates a {@link NetworkJob}, places the underlying
     * standing order, and transitions the job to {@link JobState#ACTIVE}.
     *
     * @param request describes what to source and where to deliver it
     * @return the created job (caller may hold for status queries)
     */
    public NetworkJob submit(ItemRequest request) {
        UUID jobId = UUID.randomUUID();
        NetworkJob job = new NetworkJob(jobId, request.item(), request.amount(),
                request.amount(), request.fulfillmentMode(), request.destination());
        jobs.put(jobId, job);

        // Build work orders from RequestPlanner (controller implements PlanningView)
        FulfillmentPlan plan = new RequestPlanner().plan(
                request.item(), request.amount(), request.fulfillmentMode(), controller);
        for (PlanNode node : plan.roots()) {
            if (node instanceof PlanNode.ExtractNode ext) {
                job.addWorkOrder(new WorkOrder.ExtractWorkOrder(
                        null, ext.provider(), ext.item(), ext.amount(), CommitmentLevel.PLANNED));
            } else if (node instanceof PlanNode.CraftNode craft) {
                job.addWorkOrder(new WorkOrder.CraftWorkOrder(
                        craft.crafter(), craft.output(), craft.batches(), CommitmentLevel.PLANNED));
            }
        }
        job.addWorkOrder(new WorkOrder.DeliverWorkOrder(
                request.destination(), request.item(), request.amount(), CommitmentLevel.PLANNED));

        // Place underlying order in the controller
        UUID orderId = controller.placeOrder(
                request.item(), request.amount(), request.destination(), request.fulfillmentMode());
        orderToJob.put(orderId, jobId);
        jobToOrder.put(jobId, orderId);

        job.transitionTo(JobState.ACTIVE);
        NetDbg.out("[Jobs] Submitted job {} for {}x {} | plan: {} extract/craft nodes",
                jobId.toString().substring(0, 8), request.amount(),
                request.item().toStack(1).getItem(), plan.roots().size());
        return job;
    }

    // ===== Tick / Reconciliation =====

    /**
     * Run periodic reconciliation. Called once per network tick.
     *
     * <p>Every {@value #RECONCILE_INTERVAL} ticks, checks each active job to see if its supply
     * is still available. If supply has disappeared mid-order the coordinator attempts to replan
     * from alternative sources; if no alternative exists the job is transitioned to
     * {@link JobState#FAILED} (FULL mode) or left to complete with reduced scope (PARTIAL mode).
     *
     * @param view current network supply state ({@link NetworkController} implements this)
     */
    public void tick(PlanningView view) {
        if (++ticksSinceReconcile < RECONCILE_INTERVAL) return;
        ticksSinceReconcile = 0;

        purgeTerminal();

        ReconciliationService service = new ReconciliationService();
        for (NetworkJob job : activeJobs()) {
            // Only reconcile if the underlying order is still pending (not yet dispatched).
            // Once dispatched, items are in transit and 'outstanding' no longer reflects
            // sourceable supply — we'd get false positives.
            UUID orderId = jobToOrder.get(job.id());
            if (orderId == null || !controller.hasOrder(orderId)) continue;

            ReconciliationResult result = service.reconcile(job, view);
            if (!result.hasLoss()) continue;

            long lost = result.amountLost();
            NetDbg.out("[Jobs] Reconciliation: job {} lost {}x {}",
                    job.id().toString().substring(0, 8), lost, job.item().toStack(1).getItem());

            // Replan the whole outstanding amount, not just the lost slice: the order about to be
            // cancelled covers all of it, so planning only the difference would abandon the part
            // that is still perfectly sourceable.
            Optional<FulfillmentPlan> replan = service.replan(job, job.outstanding(), view);
            if (replan.isPresent() && replan.get().plannedAmount() > 0) {
                FulfillmentPlan newPlan = replan.get();
                // Cancel stale order and place a new one for the replanned amount
                controller.cancelOrder(orderId);
                UUID newOrderId = controller.placeOrder(
                        job.item(), newPlan.plannedAmount(), job.destination(), job.fulfillmentMode());
                // Follow the plan down, or outstanding() keeps reporting the original figure and the
                // job can never reach a terminal state.
                job.setPlannedAmount(
                        job.deliveredAmount() + job.invalidatedAmount() + newPlan.plannedAmount());
                orderToJob.remove(orderId);
                adoptReplacementOrder(job, newOrderId);
                NetDbg.out("[Jobs] Replanned job {} | {} items from new sources",
                        job.id().toString().substring(0, 8), newPlan.plannedAmount());
            } else {
                // No alternative supply: record the loss and let the job settle
                job.recordInvalidation(lost);
                NetDbg.out("[Jobs] Job {} cannot be replanned | lost={} state={}",
                        job.id().toString().substring(0, 8), lost, job.state());
            }
        }
    }

    // ===== Event Handlers =====

    /**
     * Called when items physically arrive at a destination.
     * Updates the matching active job's {@code deliveredAmount}.
     */
    public void onDelivery(BlockPos requester, IItemKey item, long amount) {
        for (NetworkJob job : jobs.values()) {
            if (job.state().isTerminal()) continue;
            if (job.destination().equals(requester) && job.item().equals(item)) {
                job.recordDelivery(amount);
                NetDbg.out("[Jobs] Delivery {} | {}x {} | outstanding={}",
                        job.id().toString().substring(0, 8), amount,
                        item.toStack(1).getItem(), job.outstanding());
                return;
            }
        }
    }

    /**
     * Called when a tracked dispatch is lost after it was already sent into the pipe network.
     * The controller has already released the old in-flight accounting and created a replacement
     * order; this method keeps job-to-order indexes pointed at the retry.
     */
    public void onDeliveryFailed(UUID failedOrderId, @Nullable UUID replacementOrderId) {
        UUID jobId = orderToJob.get(failedOrderId);
        if (jobId == null || replacementOrderId == null) return;

        NetworkJob job = jobs.get(jobId);
        if (job == null || job.state().isTerminal()) return;

        orderToJob.remove(failedOrderId);
        adoptReplacementOrder(job, replacementOrderId);
        NetDbg.out("[Jobs] Delivery failed for job {} | retry order {}",
                job.id().toString().substring(0, 8),
                replacementOrderId.toString().substring(0, 8));
    }

    /**
     * Called when an order fails because its ingredient chain cannot be satisfied.
     * Implements {@link NetworkController.OrderFailureListener}.
     */
    @Override
    public void onOrderFailed(UUID orderId, BlockPos requester, IItemKey item,
                              long amount, List<IItemKey> missing) {
        UUID jobId = orderToJob.remove(orderId);
        if (jobId == null) return;
        jobToOrder.remove(jobId);
        NetworkJob job = jobs.get(jobId);
        if (job == null) return;
        job.transitionTo(JobState.FAILED);
        NetDbg.out("[Jobs] Job {} FAILED | {}x {} | missing: {}",
                job.id().toString().substring(0, 8), amount,
                item.toStack(1).getItem(), missing);
    }

    /** Points the job/order cross-indexes at {@code newOrderId} and cycles the job REPLANNING→ACTIVE to resume it. */
    private void adoptReplacementOrder(NetworkJob job, UUID newOrderId) {
        orderToJob.put(newOrderId, job.id());
        jobToOrder.put(job.id(), newOrderId);
        job.transitionTo(JobState.REPLANNING);
        job.transitionTo(JobState.ACTIVE);
    }

    // ===== Queries =====

    /** All jobs that have not yet reached a terminal state. */
    public List<NetworkJob> activeJobs() {
        List<NetworkJob> result = new ArrayList<>();
        for (NetworkJob job : jobs.values()) {
            if (!job.state().isTerminal()) result.add(job);
        }
        return Collections.unmodifiableList(result);
    }

    /** All jobs regardless of state. */
    public Collection<NetworkJob> allJobs() {
        return Collections.unmodifiableCollection(jobs.values());
    }

    // ===== Maintenance =====

    /**
     * Remove terminal jobs from the tracking map.
     * Call periodically to prevent unbounded growth.
     */
    public void purgeTerminal() {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, NetworkJob> e : jobs.entrySet()) {
            if (e.getValue().state().isTerminal()) toRemove.add(e.getKey());
        }
        for (UUID id : toRemove) {
            // The no-replan branch above can settle a job terminal while its order is still queued.
            // Dropping the job without cancelling leaves an untracked standing order and a
            // permanently inflated orderedForRequester — the figure requesters read to decide
            // whether to re-order.
            UUID orderId = jobToOrder.get(id);
            if (orderId != null) controller.cancelOrder(orderId);
            jobs.remove(id);
        }
        orderToJob.entrySet().removeIf(e -> !jobs.containsKey(e.getValue()));
        jobToOrder.entrySet().removeIf(e -> !jobs.containsKey(e.getKey()));
    }

    /**
     * Absorb jobs from another coordinator (used when two networks join).
     */
    public void merge(JobCoordinator other) {
        jobs.putAll(other.jobs);
        orderToJob.putAll(other.orderToJob);
        jobToOrder.putAll(other.jobToOrder);
    }
}
