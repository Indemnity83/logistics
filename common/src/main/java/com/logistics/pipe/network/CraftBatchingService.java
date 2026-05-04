package com.logistics.pipe.network;

import com.logistics.core.lib.network.CrafterBufferState;

/**
 * Computes the safe number of crafting batches to submit given a crafter's current buffer state.
 *
 * <p>Prevents ingredient overflow when the autocrafter's input slots are nearly full: rather than
 * ordering ingredients for the entire requested amount at once, callers cap the submission to what
 * the crafter can currently absorb. The remaining batches are submitted on subsequent ticks once
 * the crafter has consumed the current batch.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class CraftBatchingService {

    /**
     * Compute the safe number of batches to submit.
     *
     * <p>Converts {@code totalNeeded} into batches, then clamps to the crafter's current buffer
     * capacity. Both limits are respected: if the buffer can only accept 4 batches, at most 4
     * batches are returned regardless of how many were requested.
     *
     * @param totalNeeded    total output items needed
     * @param outputPerBatch items produced per recipe batch (must be &gt; 0)
     * @param buffer         current crafter input/output buffer limits
     * @return safe number of batches to submit now (0 if crafter has no capacity)
     */
    public long safeBatchCount(long totalNeeded, long outputPerBatch, CrafterBufferState buffer) {
        if (outputPerBatch <= 0) return 0;
        long requestedBatches = (totalNeeded + outputPerBatch - 1) / outputPerBatch;
        long capacityBatches = buffer.safeBatchCapacity();
        return Math.min(requestedBatches, capacityBatches);
    }
}
