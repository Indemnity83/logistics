package com.logistics.pipe.network;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.storage.IItemKey;
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
 *   <li>{@link #plannedAmount()}   — what the network committed to source</li>
 *   <li>{@link #deliveredAmount()} — what has physically arrived at the destination</li>
 * </ul>
 *
 * <p>Mutable state; mutated only by {@link JobCoordinator}.
 */
public final class NetworkJob {

    private final UUID id;
    private final IItemKey item;
    private final long requestedAmount;
    private long plannedAmount;
    private long deliveredAmount;
    private long invalidatedAmount;
    private final FulfillmentMode fulfillmentMode;
    private final BlockPos destination;
    private final List<WorkOrder> workOrders = new ArrayList<>();
    private JobState state;

    NetworkJob(UUID id, IItemKey item, long requestedAmount, long plannedAmount,
               FulfillmentMode fulfillmentMode, BlockPos destination) {
        this.id = id;
        this.item = item;
        this.requestedAmount = requestedAmount;
        this.plannedAmount = plannedAmount;
        this.fulfillmentMode = fulfillmentMode;
        this.destination = destination;
        this.state = JobState.PLANNED;
    }

    public UUID id() { return id; }
    public IItemKey item() { return item; }
    public FulfillmentMode fulfillmentMode() { return fulfillmentMode; }
    public BlockPos destination() { return destination; }
    public long requestedAmount() { return requestedAmount; }
    public long plannedAmount() { return plannedAmount; }
    public long deliveredAmount() { return deliveredAmount; }
    public long invalidatedAmount() { return invalidatedAmount; }

    public long outstanding() {
        return Math.max(0L, plannedAmount - deliveredAmount - invalidatedAmount);
    }

    public JobState state() { return state; }

    public List<WorkOrder> workOrders() {
        return Collections.unmodifiableList(workOrders);
    }

    void addWorkOrder(WorkOrder order) { workOrders.add(order); }
    void setPlannedAmount(long amount) { this.plannedAmount = amount; }

    void transitionTo(JobState newState) {
        if (state.isTerminal()) return;
        this.state = newState;
    }

    void recordDelivery(long amount) {
        if (amount <= 0) return;
        deliveredAmount += amount;
        if (outstanding() <= 0) state = JobState.COMPLETE;
        else state = JobState.DELIVERING;
    }

    void recordInvalidation(long amount) {
        if (amount <= 0) return;
        invalidatedAmount += amount;
        if (outstanding() <= 0 && deliveredAmount == 0) state = JobState.FAILED;
        else if (outstanding() <= 0) state = JobState.COMPLETE;
    }

    @Override
    public String toString() {
        return "NetworkJob[" + id.toString().substring(0, 8)
                + " " + item.toStack(1).getItem()
                + " req=" + requestedAmount + " plan=" + plannedAmount
                + " del=" + deliveredAmount + " inv=" + invalidatedAmount
                + " " + state + "]";
    }
}
