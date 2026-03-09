package com.logistics.pipe.network;

/**
 * How committed a {@link WorkOrder} is within its execution lifecycle.
 * Progresses from loosely-planned toward fully-completed.
 */
public enum CommitmentLevel {
    /** Identified as a required step but no resources committed yet. */
    PLANNED,
    /** Supply reserved against a provider (see {@link ReservationId}). */
    RESERVED,
    /** Ingredients dispatched into a crafter's input buffer. */
    DISPATCHED_INPUT,
    /** Physical extraction complete; items in transit. */
    IN_PROGRESS,
    /** Delivery confirmed. */
    COMPLETED
}
