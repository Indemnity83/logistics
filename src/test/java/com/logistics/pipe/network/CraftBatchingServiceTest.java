package com.logistics.pipe.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CraftBatchingService}.
 *
 * <p>Pure Java — no Minecraft bootstrap required.
 */
class CraftBatchingServiceTest {

    private final CraftBatchingService service = new CraftBatchingService();

    // ===== Basic batch calculations =====

    @Test
    void testExactFit_returnsRequestedBatches() {
        // 16 needed, 4 per batch → 4 batches; buffer has capacity 10 → clamp to 4
        long result = service.safeBatchCount(16, 4, new CrafterBufferState(10, 10));
        assertEquals(4, result);
    }

    @Test
    void testCeilingRounding_batchesRoundUp() {
        // 9 needed, 4 per batch → ceil(9/4) = 3 batches; buffer has capacity 10
        long result = service.safeBatchCount(9, 4, new CrafterBufferState(10, 10));
        assertEquals(3, result);
    }

    @Test
    void testSingleItemPerBatch_matchesNeeded() {
        long result = service.safeBatchCount(8, 1, new CrafterBufferState(10, 10));
        assertEquals(8, result);
    }

    // ===== Buffer capacity clamping =====

    @Test
    void testBufferLimitsInserts_clampsDown() {
        // 20 needed, 1 per batch → 20 batches; buffer maxInsertsNow = 5
        long result = service.safeBatchCount(20, 1, new CrafterBufferState(5, 100));
        assertEquals(5, result);
    }

    @Test
    void testBufferLimitsOutputSpace_clampsDown() {
        // 20 batches requested, but output space only allows 3
        long result = service.safeBatchCount(20, 1, new CrafterBufferState(100, 3));
        assertEquals(3, result);
    }

    @Test
    void testBothLimitsApply_minimumWins() {
        // maxInsertsNow=4, maxCraftsFromOutputSpace=2 → safeBatchCapacity=2
        long result = service.safeBatchCount(100, 1, new CrafterBufferState(4, 2));
        assertEquals(2, result);
    }

    @Test
    void testFullBuffer_returnsZero() {
        long result = service.safeBatchCount(16, 4, CrafterBufferState.FULL);
        assertEquals(0, result);
    }

    @Test
    void testUnlimitedBuffer_returnsRequestedBatches() {
        long result = service.safeBatchCount(64, 4, CrafterBufferState.UNLIMITED);
        assertEquals(16, result);
    }

    // ===== Edge / guard cases =====

    @Test
    void testZeroOutputPerBatch_returnsZero() {
        long result = service.safeBatchCount(16, 0, CrafterBufferState.UNLIMITED);
        assertEquals(0, result, "outputPerBatch=0 should return 0 to avoid division by zero");
    }

    @Test
    void testNegativeOutputPerBatch_returnsZero() {
        long result = service.safeBatchCount(16, -1, CrafterBufferState.UNLIMITED);
        assertEquals(0, result);
    }

    @Test
    void testOneNeeded_onePerBatch_oneCapacity() {
        long result = service.safeBatchCount(1, 1, new CrafterBufferState(1, 1));
        assertEquals(1, result);
    }

    @Test
    void testLargeRequest_cappedByBuffer() {
        // 1000 items, 4 per batch → 250 batches; crafter can only take 8
        long result = service.safeBatchCount(1000, 4, new CrafterBufferState(8, 8));
        assertEquals(8, result);
    }
}
