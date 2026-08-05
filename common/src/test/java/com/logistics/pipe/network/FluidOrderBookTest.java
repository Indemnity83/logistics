package com.logistics.pipe.network;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FluidOrderBook}. Mirrors the shape of {@link NetworkControllerTest} for the
 * fluid-native bookkeeping path, with additional coverage for the specific correctness fixes called
 * out during review: provider reservation (unshipped-only release), validated delivery/failure
 * acknowledgement, and merge() rebuilding outstanding totals from source instead of blindly summing.
 */
class FluidOrderBookTest extends MinecraftTestEnvironment {

    private FluidOrderBook book;
    private static final BlockPos PROVIDER1 = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(10, 0, 0);
    private static final BlockPos REQUESTER = new BlockPos(5, 0, 0);
    private static final BlockPos REQUESTER2 = new BlockPos(6, 0, 0);

    @BeforeEach
    void setUp() {
        book = new FluidOrderBook();
    }

    // ===== Supply Registration =====

    @Test
    void registerSupply_effectiveAvailable() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        assertEquals(5000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER));
        assertEquals(5000L, book.getAvailableMbFor(Fluids.WATER));
    }

    @Test
    void registerSupply_refresh_replacesRawAndClearsCommitment() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 1000L, REQUESTER);
        book.nextDispatchable(); // reserves 1000 mB against PROVIDER1

        book.registerSupply(PROVIDER1, Fluids.WATER, 3000L, 1); // fresh scan supersedes the reservation
        assertEquals(3000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER),
                "A fresh scan should replace raw supply and clear the stale commitment");
    }

    @Test
    void removeSupply_clearsAvailabilityAndCommitment() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.removeSupply(PROVIDER1);
        assertEquals(0L, book.effectiveAvailable(PROVIDER1, Fluids.WATER));
        assertEquals(0L, book.getAvailableMbFor(Fluids.WATER));
    }

    // ===== placeOrder validation =====

    @Test
    void placeOrder_rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> book.placeOrder(Fluids.WATER, 0L, REQUESTER));
        assertThrows(IllegalArgumentException.class, () -> book.placeOrder(Fluids.WATER, -1L, REQUESTER));
    }

    @Test
    void placeOrder_incrementsOutstandingImmediately() {
        UUID orderId = book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        assertNotNull(orderId);
        assertEquals(500L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    // ===== nextDispatchable / dispatch =====

    @Test
    void nextDispatchable_fullFill() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(orderId, cmd.orderId());
        assertEquals(PROVIDER1, cmd.provider());
        assertEquals(REQUESTER, cmd.requester());
        assertEquals(Fluids.WATER, cmd.fluid());
        assertEquals(2000L, cmd.amountMb());
    }

    @Test
    void nextDispatchable_partialFill_capsAtAvailable() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 500L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(500L, cmd.amountMb(), "Partial dispatch should be capped to available stock");
    }

    @Test
    void nextDispatchable_noSupply_returnsNull() {
        book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        assertNull(book.nextDispatchable());
    }

    @Test
    void nextDispatchable_exactNonRoundAmount_dispatchesExactly137() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 137L, REQUESTER);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(137L, cmd.amountMb(), "A non-round order must not be rounded to any packet multiple");

        book.recordDispatched(cmd, 137L);
        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 137L);
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void nextDispatchable_multiProvider_triesInPriorityOrder() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 500L, 5); // lower priority
        book.registerSupply(PROVIDER2, Fluids.WATER, 5000L, 1); // higher priority (preferred)
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(PROVIDER2, cmd.provider(), "Higher-priority provider should be preferred");
    }

    // ===== Provider reservation (the "double-allocation" correctness fix) =====

    @Test
    void reservation_secondDispatchInSameTickCannotDoubleAllocate() {
        // Only 1000 mB available; two orders of 1000 each — only one should be dispatchable before rescan.
        book.registerSupply(PROVIDER1, Fluids.WATER, 1000L, 1);
        book.placeOrder(Fluids.WATER, 1000L, REQUESTER);
        book.placeOrder(Fluids.WATER, 1000L, REQUESTER2);

        FluidOrderBook.FluidDispatchCommand first = book.nextDispatchable();
        assertNotNull(first, "First order should be dispatchable");

        FluidOrderBook.FluidDispatchCommand second = book.nextDispatchable();
        assertNull(second, "Second order must not double-allocate the same tank contents before a rescan");
    }

    @Test
    void reservation_recordDispatched_releasesOnlyUnshippedPortion() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(3000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER),
                "Reserving 2000 out of 5000 raw should leave 3000 effective");

        book.recordDispatched(cmd, 800L); // shipped less than reserved (energy/extraction shortfall)

        // Released: the unshipped delta (2000 - 800 = 1200) becomes available again immediately.
        // NOT released: the shipped 800 mB stays committed until the next scan (it was actually
        // extracted but rawAvailableMb hasn't been refreshed to reflect that yet), so effective
        // availability is raw(5000) - committed(800) = 4200, not the full 5000.
        assertEquals(4200L, book.effectiveAvailable(PROVIDER1, Fluids.WATER),
                "Unshipped portion (1200) released; only the shipped 800 stays committed");
    }

    @Test
    void reservation_zeroShipped_releasesEntireReservation() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);

        book.recordDispatched(cmd, 0L);

        assertEquals(5000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER),
                "Zero shipped means nothing was extracted, so the entire reservation is released");
    }

    @Test
    void reservation_shippedPortionStaysCommittedUntilNextScan() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L); // fully shipped — 2000 mB physically extracted

        // Even though rawAvailableMb is still stale (5000), effective availability already correctly
        // reflects the true remaining amount (5000 - 2000 committed = 3000) — the shipped portion
        // deliberately stays committed rather than being released by recordDispatched.
        assertEquals(3000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER));

        // The next scan reports the true remaining amount directly, superseding the commitment.
        book.registerSupply(PROVIDER1, Fluids.WATER, 3000L, 1);
        assertEquals(3000L, book.effectiveAvailable(PROVIDER1, Fluids.WATER),
                "A fresh scan reporting the true amount should agree with the derived figure");
    }

    @Test
    void reservation_orderQueueAndInTransitReflectShippedAmount() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 800L); // partial

        // Outstanding is unaffected by recordDispatched (still queued+in-flight = 2000).
        assertEquals(2000L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        // The remaining 1200 mB is still queued and dispatchable once supply is available again.
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        FluidOrderBook.FluidDispatchCommand cmd2 = book.nextDispatchable();
        assertNotNull(cmd2, "Remaining order should still be dispatchable");
        assertEquals(orderId, cmd2.orderId());
        assertEquals(1200L, cmd2.amountMb());
    }

    // ===== notifyDelivery / notifyDeliveryFailed (validated acknowledgement) =====

    @Test
    void notifyDelivery_decrementsOutstanding() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 2000L);

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void notifyDelivery_rejectsUnknownDeliveryId() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER);

        book.notifyDelivery(UUID.randomUUID(), REQUESTER, Fluids.WATER, 2000L);

        assertEquals(2000L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "An acknowledgement for an unknown delivery id must not mutate outstanding totals");
    }

    @Test
    void notifyDelivery_rejectsDuplicateAcknowledgement() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 2000L);
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        // Replay the exact same acknowledgement — must be rejected, not driven negative.
        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 2000L);
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void notifyDelivery_rejectsOversizedAmount_andDoesNotAffectUnrelatedDemand() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        // A second, unrelated order for a different fluid/requester.
        book.registerSupply(PROVIDER2, Fluids.LAVA, 5000L, 1);
        book.placeOrder(Fluids.LAVA, 1000L, REQUESTER2);

        // Oversized report against the first delivery id — must be rejected, not partially applied.
        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 9999L);

        assertEquals(2000L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Oversized acknowledgement must not reduce outstanding at all");
        assertEquals(1000L, book.getOrderedAmountFor(REQUESTER2, Fluids.LAVA),
                "Unrelated demand must be completely unaffected");
    }

    @Test
    void notifyDelivery_rejectsMismatchedRequesterOrFluid() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        book.notifyDelivery(orderId, REQUESTER2, Fluids.WATER, 2000L); // wrong requester
        book.notifyDelivery(orderId, REQUESTER, Fluids.LAVA, 2000L); // wrong fluid

        assertEquals(2000L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Mismatched acknowledgements must be rejected, not applied");
    }

    @Test
    void notifyDelivery_partialRelease_resolvesOnlyItsOwnPayload() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 800L); // one packet's worth
        assertEquals(1200L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 1200L); // the remaining packet(s)
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void notifyDeliveryFailed_doesNotAutoRequeue() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 2000L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 2000L);

        book.notifyDeliveryFailed(orderId, REQUESTER, Fluids.WATER, 2000L);

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Failure releases outstanding accounting");
        assertNull(book.nextDispatchable(), "Failure must not automatically requeue a new order");
    }

    @Test
    void notifyDeliveryFailedNoId_clampsToOutstanding() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 500L, REQUESTER);

        book.notifyDeliveryFailedNoId(REQUESTER, Fluids.WATER, 9999L); // reports more than outstanding

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Should clamp to outstanding, never drive it negative");
    }

    @Test
    void notifyDeliveryFailedNoId_doesNotAffectUnrelatedDemand() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        book.registerSupply(PROVIDER2, Fluids.LAVA, 5000L, 1);
        book.placeOrder(Fluids.LAVA, 1000L, REQUESTER2);

        book.notifyDeliveryFailedNoId(REQUESTER, Fluids.WATER, 500L);

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
        assertEquals(1000L, book.getOrderedAmountFor(REQUESTER2, Fluids.LAVA));
    }

    // ===== cancelOrder / cancelOrdersFor =====

    @Test
    void cancelOrder_removesQueuedOrderAndDecrements() {
        UUID orderId = book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        book.cancelOrder(orderId);
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void cancelOrder_removesInTransitOrderAndDecrements() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 500L);

        book.cancelOrder(orderId);

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void cancelOrdersFor_removesAllOrdersForRequesterOnly() {
        book.placeOrder(Fluids.WATER, 500L, REQUESTER);
        book.placeOrder(Fluids.LAVA, 300L, REQUESTER);
        book.placeOrder(Fluids.WATER, 200L, REQUESTER2);

        book.cancelOrdersFor(REQUESTER);

        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.LAVA));
        assertEquals(200L, book.getOrderedAmountFor(REQUESTER2, Fluids.WATER));
    }

    // ===== FulfillmentMode =====

    @Test
    void fullMode_blocksPartialDispatchWhenNetworkStockInsufficient() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 500L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER, FulfillmentMode.FULL);

        assertNull(book.nextDispatchable(), "FULL mode should not dispatch when total stock < requested");
    }

    @Test
    void fullMode_dispatchesWhenExactStockAvailable() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 2000L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER, FulfillmentMode.FULL);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(2000L, cmd.amountMb());
    }

    @Test
    void partialMode_dispatchesPartialWhenStockInsufficient() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 500L, 1);
        book.placeOrder(Fluids.WATER, 2000L, REQUESTER, FulfillmentMode.PARTIAL);

        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(500L, cmd.amountMb());
    }

    // ===== Multi-tick partial dispatch reconciliation =====

    @Test
    void multiTickPartialDispatch_mixedDeliveryAndFailureReconcilesExactly() {
        // Simulates the same order being dispatched across three separate ticks/cycles.
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 900L, REQUESTER);
        assertEquals(900L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        // Tick 1: dispatch 300, rescan to release the reservation for the next attempt.
        FluidOrderBook.FluidDispatchCommand cmd1 = book.nextDispatchable();
        assertEquals(900L, cmd1.amountMb());
        book.recordDispatched(cmd1, 300L);
        assertEquals(900L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER), "Still fully outstanding — in flight");
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1); // rescan between ticks

        // Tick 2: dispatch another 300 of the remaining 600.
        FluidOrderBook.FluidDispatchCommand cmd2 = book.nextDispatchable();
        assertEquals(600L, cmd2.amountMb());
        book.recordDispatched(cmd2, 300L);
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);

        // Tick 3: dispatch the final 300.
        FluidOrderBook.FluidDispatchCommand cmd3 = book.nextDispatchable();
        assertEquals(300L, cmd3.amountMb());
        book.recordDispatched(cmd3, 300L);

        assertNull(book.nextDispatchable(), "Order should be fully dispatched now");
        assertEquals(900L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER), "Still outstanding until delivery/failure");

        // Resolve the three physical packets: two delivered, one fails.
        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 300L);
        assertEquals(600L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        book.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 300L);
        assertEquals(300L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        book.notifyDeliveryFailed(orderId, REQUESTER, Fluids.WATER, 300L);
        assertEquals(0L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Every portion resolved (delivered or failed) — outstanding must reach exactly zero");
    }

    // ===== merge =====

    @Test
    void merge_combinesSupplyAndOrders() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        book.placeOrder(Fluids.WATER, 1000L, REQUESTER);

        FluidOrderBook other = new FluidOrderBook();
        other.registerSupply(PROVIDER2, Fluids.LAVA, 3000L, 1);
        other.placeOrder(Fluids.LAVA, 500L, REQUESTER2);

        book.merge(other);

        assertEquals(5000L, book.getAvailableMbFor(Fluids.WATER));
        assertEquals(3000L, book.getAvailableMbFor(Fluids.LAVA));
        assertEquals(1000L, book.getOrderedAmountFor(REQUESTER, Fluids.WATER));
        assertEquals(500L, book.getOrderedAmountFor(REQUESTER2, Fluids.LAVA));
    }

    @Test
    void merge_preservesInFlightOrderAcrossQueuedAndInTransitPortions() {
        book.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        UUID orderId = book.placeOrder(Fluids.WATER, 900L, REQUESTER);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, 300L); // 300 in transit, 600 still queued

        FluidOrderBook other = new FluidOrderBook();
        other.merge(book); // merge "book" (with the in-flight order) into a fresh, empty book

        assertEquals(900L, other.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "Merged book should preserve queued + in-transit total exactly");

        // The original delivery id should still resolve correctly post-merge.
        other.notifyDelivery(orderId, REQUESTER, Fluids.WATER, 300L);
        assertEquals(600L, other.getOrderedAmountFor(REQUESTER, Fluids.WATER));
    }

    @Test
    void merge_overlappingOrderUuidDoesNotDoubleCountDemand() {
        // Simulate a network split-then-rejoin: both books independently remember the same order id.
        UUID sharedOrderId = UUID.randomUUID();

        FluidOrderBook bookA = new FluidOrderBook();
        bookA.registerSupply(PROVIDER1, Fluids.WATER, 5000L, 1);
        bookA.placeOrder(sharedOrderId, Fluids.WATER, 700L, REQUESTER, FulfillmentMode.PARTIAL);

        FluidOrderBook bookB = new FluidOrderBook();
        bookB.placeOrder(sharedOrderId, Fluids.WATER, 700L, REQUESTER, FulfillmentMode.PARTIAL);

        assertEquals(700L, bookA.getOrderedAmountFor(REQUESTER, Fluids.WATER));
        assertEquals(700L, bookB.getOrderedAmountFor(REQUESTER, Fluids.WATER));

        bookA.merge(bookB);

        assertEquals(700L, bookA.getOrderedAmountFor(REQUESTER, Fluids.WATER),
                "A shared order UUID present in both books must count once, not twice, after merge");
    }
}
