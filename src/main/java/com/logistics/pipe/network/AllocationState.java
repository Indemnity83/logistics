package com.logistics.pipe.network;

/**
 * Lifecycle state of an {@link ItemReservation}.
 *
 * <pre>
 *   PLANNED ──► RESERVED ──► EXTRACTING ──► IN_TRANSIT ──► DELIVERED
 *                   │                            │
 *                   └────────────────────────────┴──► INVALIDATED
 * </pre>
 *
 * {@link #INVALIDATED} reservations are candidates for replanning in a future phase.
 */
public enum AllocationState {
    /** Reservation planned but not yet committed to a specific provider. */
    PLANNED,
    /** Provider identified and stock soft-reserved; awaiting physical extraction. */
    RESERVED,
    /** Extraction in progress (physical pull from inventory has started). */
    EXTRACTING,
    /** Items physically extracted and traveling through pipes toward the destination. */
    IN_TRANSIT,
    /** Items delivered to the requester inventory. Terminal success state. */
    DELIVERED,
    /** Reservation could not be fulfilled (provider gone, stock disappeared, order cancelled). Terminal failure state. */
    INVALIDATED;

    /** {@code true} for states where the reservation still holds stock against a provider. */
    public boolean isActive() {
        return this == PLANNED || this == RESERVED || this == EXTRACTING || this == IN_TRANSIT;
    }
}
