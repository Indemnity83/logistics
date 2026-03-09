package com.logistics.pipe.network;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A long-lived logistics job tracking the full lifecycle of sourcing and delivering items.
 *
 * <p>Tracks amounts at three resolution levels:
 * <ul>
 *   <li>{@link #requestedAmount()} — what the requester asked for</li>
 *   <li>{@link #plannedAmount()}   — what the network committed to source (≤ requested for PARTIAL jobs)</li>
 *   <li>{@link #deliveredAmount()} — what has physically arrived at the destination</li>
 * </ul>
 *
 * <p>{@link #outstanding()} = plannedAmount − deliveredAmount − invalidatedAmount.
 * When outstanding reaches 0 the job transitions to {@link JobState#COMPLETE}.
 *
 * <p>Mutable state; mutated only by {@link JobCoordinator}. Created via
 * {@link JobCoordinator#submit(ItemRequest)}.
 *
 * <p>Zero Minecraft API coupling beyond BlockPos/ItemVariant.
 */
public final class NetworkJob {

    private final UUID id;
    private final ItemVariant item;
    private final long requestedAmount;
    private long plannedAmount;
    private long deliveredAmount;
    private long invalidatedAmount;
    private final FulfillmentMode fulfillmentMode;
    private final BlockPos destination;
    private final List<WorkOrder> workOrders = new ArrayList<>();
    private JobState state;

    NetworkJob(UUID id, ItemVariant item, long requestedAmount, long plannedAmount,
               FulfillmentMode fulfillmentMode, BlockPos destination) {
        this.id = id;
        this.item = item;
        this.requestedAmount = requestedAmount;
        this.plannedAmount = plannedAmount;
        this.fulfillmentMode = fulfillmentMode;
        this.destination = destination;
        this.state = JobState.PLANNED;
    }

    // ===== Identity =====

    public UUID id() { return id; }
    public ItemVariant item() { return item; }
    public FulfillmentMode fulfillmentMode() { return fulfillmentMode; }
    public BlockPos destination() { return destination; }

    // ===== Amount tracking =====

    public long requestedAmount() { return requestedAmount; }
    public long plannedAmount() { return plannedAmount; }
    public long deliveredAmount() { return deliveredAmount; }
    public long invalidatedAmount() { return invalidatedAmount; }

    /**
     * Amount still expected to arrive: planned − delivered − invalidated.
     * Zero when the job is complete or fully failed.
     */
    public long outstanding() {
        return Math.max(0L, plannedAmount - deliveredAmount - invalidatedAmount);
    }

    // ===== State =====

    public JobState state() { return state; }

    // ===== Work orders =====

    public List<WorkOrder> workOrders() {
        return Collections.unmodifiableList(workOrders);
    }

    // ===== Package-private mutation (JobCoordinator only) =====

    void addWorkOrder(WorkOrder order) {
        workOrders.add(order);
    }

    void setPlannedAmount(long amount) {
        this.plannedAmount = amount;
    }

    void transitionTo(JobState newState) {
        if (state.isTerminal()) return; // no transitions out of terminal states
        this.state = newState;
    }

    void recordDelivery(long amount) {
        if (amount <= 0) return;
        deliveredAmount += amount;
        if (outstanding() <= 0) {
            state = JobState.COMPLETE;
        } else {
            state = JobState.DELIVERING;
        }
    }

    void recordInvalidation(long amount) {
        if (amount <= 0) return;
        invalidatedAmount += amount;
        if (outstanding() <= 0 && deliveredAmount == 0) {
            state = JobState.FAILED;
        } else if (outstanding() <= 0) {
            state = JobState.COMPLETE; // partially delivered is good enough for PARTIAL jobs
        }
    }

    @Override
    public String toString() {
        return "NetworkJob[" + id.toString().substring(0, 8)
                + " " + item.toStack().getItem()
                + " req=" + requestedAmount + " plan=" + plannedAmount
                + " del=" + deliveredAmount + " inv=" + invalidatedAmount
                + " " + state + "]";
    }
}
