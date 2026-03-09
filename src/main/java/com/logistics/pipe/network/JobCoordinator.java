package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates {@link NetworkJob} lifecycle on top of {@link NetworkController}.
 *
 * <p>Modules submit an {@link ItemRequest} via {@link #submit}; the coordinator creates a
 * {@link NetworkJob}, places the underlying order into {@link NetworkController}, and tracks
 * delivery progress. When all items arrive the job transitions to
 * {@link JobState#COMPLETE} automatically.
 *
 * <p>Phase 5 scope: job creation, delivery tracking, failure notification.
 * Full planning ({@code RequestPlanner}) and reconciliation are added in later phases.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class JobCoordinator implements NetworkController.OrderFailureListener {

    private final NetworkController controller;

    // Primary store: jobId → job (insertion order = submission order)
    private final Map<UUID, NetworkJob> jobs = new LinkedHashMap<>();

    // Cross-index: orderId → jobId (for failure callbacks)
    private final Map<UUID, UUID> orderToJob = new HashMap<>();

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

        job.transitionTo(JobState.ACTIVE);
        NetDbg.out("[Jobs] Submitted job {} for {}x {} | plan: {} extract/craft nodes",
                jobId.toString().substring(0, 8), request.amount(),
                request.item().toStack().getItem(), plan.roots().size());
        return job;
    }

    // ===== Event Handlers =====

    /**
     * Called when items physically arrive at a destination.
     * Updates the matching active job's {@code deliveredAmount}.
     */
    public void onDelivery(BlockPos requester, ItemVariant item, long amount) {
        for (NetworkJob job : jobs.values()) {
            if (job.state().isTerminal()) continue;
            if (job.destination().equals(requester) && job.item().equals(item)) {
                job.recordDelivery(amount);
                NetDbg.out("[Jobs] Delivery {} | {}x {} | outstanding={}",
                        job.id().toString().substring(0, 8), amount,
                        item.toStack().getItem(), job.outstanding());
                return;
            }
        }
    }

    /**
     * Called when an order fails because its ingredient chain cannot be satisfied.
     * Implements {@link NetworkController.OrderFailureListener}.
     */
    @Override
    public void onOrderFailed(UUID orderId, BlockPos requester, ItemVariant item,
                              long amount, List<ItemVariant> missing) {
        UUID jobId = orderToJob.remove(orderId);
        if (jobId == null) return;
        NetworkJob job = jobs.get(jobId);
        if (job == null) return;
        job.transitionTo(JobState.FAILED);
        NetDbg.out("[Jobs] Job {} FAILED | {}x {} | missing: {}",
                job.id().toString().substring(0, 8), amount,
                item.toStack().getItem(), missing);
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
            jobs.remove(id);
        }
        orderToJob.entrySet().removeIf(e -> !jobs.containsKey(e.getValue()));
    }

    /**
     * Absorb jobs from another coordinator (used when two networks join).
     */
    public void merge(JobCoordinator other) {
        jobs.putAll(other.jobs);
        orderToJob.putAll(other.orderToJob);
    }
}
