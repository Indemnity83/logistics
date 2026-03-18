package com.logistics.core.lib.network;

/**
 * Current buffer capacity of an autocrafter adjacent to a crafting pipe.
 * Used to limit how many batches can be submitted without overflowing the crafter's
 * input or output inventory.
 *
 * <p>Pure value object — no Minecraft world coupling.
 */
public record CrafterBufferState(int maxInsertsNow, int maxCraftsFromOutputSpace) {
    public CrafterBufferState {
        if (maxInsertsNow < 0) throw new IllegalArgumentException("maxInsertsNow must not be negative");
        if (maxCraftsFromOutputSpace < 0) throw new IllegalArgumentException("maxCraftsFromOutputSpace must not be negative");
    }

    /** A state where the crafter has no capacity — used when the buffer cannot be queried. */
    public static final CrafterBufferState FULL = new CrafterBufferState(0, 0);

    /** A state representing effectively unlimited capacity (creative/test use). */
    public static final CrafterBufferState UNLIMITED = new CrafterBufferState(Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** How many batches can safely be submitted right now (minimum of the two limits). */
    public int safeBatchCapacity() {
        return Math.min(maxInsertsNow, maxCraftsFromOutputSpace);
    }
}
