package com.logistics.pipe.network;

/**
 * Lifecycle state of a {@link NetworkJob}.
 *
 * <pre>
 *   PLANNED ──► ACTIVE ──► WAITING_INPUTS ──► DELIVERING ──► COMPLETE
 *                 │                                │
 *                 └──────────► REPLANNING ─────────┘
 *                 │
 *                 └──► FAILED
 *                 └──► CANCELLED
 * </pre>
 */
public enum JobState {
    /** Job created but not yet submitted to the network. */
    PLANNED,
    /** Order placed; network is working to source items. */
    ACTIVE,
    /** Waiting for crafting ingredients to arrive before crafting can start. */
    WAITING_INPUTS,
    /** Items extracted; traveling toward the destination. */
    DELIVERING,
    /** Replanning after stock loss or failed delivery; will re-enter ACTIVE if successful. */
    REPLANNING,
    /** All requested items delivered. Terminal success state. */
    COMPLETE,
    /** Cannot be fulfilled (missing ingredients, no supply, FULL mode unsatisfiable). Terminal failure state. */
    FAILED,
    /** Explicitly cancelled by the requester. Terminal state. */
    CANCELLED;

    /** {@code true} for terminal states that require no further processing. */
    public boolean isTerminal() {
        return this == COMPLETE || this == FAILED || this == CANCELLED;
    }
}
