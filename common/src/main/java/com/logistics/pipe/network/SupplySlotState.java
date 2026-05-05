package com.logistics.pipe.network;

/**
 * State of a single configured slot in a supplier pipe.
 * Captures the full demand picture: desired target, current on-hand, items already in transit,
 * items with outstanding orders, and the mode config that governs when to restock.
 *
 * <p>The demand formula expressed as data:
 * <pre>  needed = desired - onHand - inbound - ordered</pre>
 * (where {@code inbound} is items already traveling toward this supplier but not yet delivered,
 * and {@code ordered} is the amount tracked by the network as pending but not yet dispatched)
 *
 * <p>Pure value object — no Minecraft world coupling.
 */
public record SupplySlotState(
        String itemId,
        long desired,
        long onHand,
        long inbound,
        long ordered,
        SupplierModeConfig config) {

    public SupplySlotState {
        if (itemId == null || itemId.isEmpty()) throw new IllegalArgumentException("itemId must not be blank");
        if (desired < 0) throw new IllegalArgumentException("desired must not be negative");
        if (onHand < 0) throw new IllegalArgumentException("onHand must not be negative");
        if (inbound < 0) throw new IllegalArgumentException("inbound must not be negative");
        if (ordered < 0) throw new IllegalArgumentException("ordered must not be negative");
        if (config == null) throw new NullPointerException("config must not be null");
    }

    /** How many more items are needed to satisfy this slot's target. */
    public long needed() {
        return Math.max(0L, desired - onHand - inbound - ordered);
    }

    /** Whether a restock request should be placed given current on-hand stock. */
    public boolean shouldRestock() {
        return needed() > 0 && config.isTriggerMet(onHand, desired);
    }
}
