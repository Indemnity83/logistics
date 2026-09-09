package com.logistics.pipe.network;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReservationManager}.
 */
class ReservationManagerTest extends MinecraftTestEnvironment {

    private ReservationManager manager;

    private static final BlockPos PROVIDER = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(10, 0, 0);
    private static final BlockPos REQUESTER = new BlockPos(5, 0, 0);
    private static final BlockPos REQUESTER2 = new BlockPos(6, 0, 0);

    @BeforeEach
    void setUp() {
        manager = new ReservationManager();
    }

    private IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private IItemKey emerald() {
        return new TestItemKey(Items.EMERALD);
    }

    // ===== Reserve + effectiveAvailable =====

    @Test
    void testReserve_reducesEffectiveAvailable() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        // raw=64, reserved=10 → effective=54
        assertEquals(54, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testNoReservations_effectiveEqualsRaw() {
        assertEquals(100, manager.effectiveAvailable(PROVIDER, diamond(), 100));
    }

    @Test
    void testMultipleReservations_sumDeducted() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(o2, PROVIDER, REQUESTER2, diamond(), 20, false);
        assertEquals(34, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testReservationsDoNotCrossItems() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 32, true);
        // Querying emerald should not be affected
        assertEquals(16, manager.effectiveAvailable(PROVIDER, emerald(), 16));
    }

    @Test
    void testReservationsDoNotCrossProviders() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 32, true);
        // Querying PROVIDER2 should not be affected
        assertEquals(64, manager.effectiveAvailable(PROVIDER2, diamond(), 64));
    }

    @Test
    void testEffectiveAvailable_clampsToZeroWhenOverReserved() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 50, true);
        manager.reserve(o2, PROVIDER, REQUESTER2, diamond(), 50, true);
        // reserved=100 > raw=64 → clamp to 0
        assertEquals(0, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    // ===== Release =====

    @Test
    void testRelease_removesReservation() {
        UUID orderId = UUID.randomUUID();
        ReservationId rid = manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.release(rid);
        assertEquals(64, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testReleaseByOrder_removesAllForOrder() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(orderId, PROVIDER2, REQUESTER, emerald(), 5, false);
        manager.releaseByOrder(orderId);
        assertEquals(64, manager.effectiveAvailable(PROVIDER, diamond(), 64));
        assertEquals(16, manager.effectiveAvailable(PROVIDER2, emerald(), 16));
    }

    @Test
    void testReleaseByOrder_doesNotAffectOtherOrders() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(o2, PROVIDER, REQUESTER2, diamond(), 20, true);
        manager.releaseByOrder(o1);
        assertEquals(44, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    // ===== Transition =====

    @Test
    void testTransition_toInTransit_stillActive() {
        UUID orderId = UUID.randomUUID();
        ReservationId rid = manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.transition(rid, AllocationState.IN_TRANSIT);
        // IN_TRANSIT is still active → still counted against effective available
        assertEquals(54, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testTransitionByOrder_toInTransit() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.transitionByOrder(orderId, AllocationState.IN_TRANSIT);
        // Should still be counted (IN_TRANSIT is active)
        assertEquals(54, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    // ===== Invalidate =====

    @Test
    void testInvalidate_noLongerCountsAgainstAvailable() {
        UUID orderId = UUID.randomUUID();
        ReservationId rid = manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 32, true);
        manager.invalidate(rid);
        // INVALIDATED is not active → not deducted
        assertEquals(64, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testInvalidateByProvider_invalidatesAllActiveForProvider() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(o2, PROVIDER, REQUESTER2, diamond(), 20, true);
        manager.invalidateByProvider(PROVIDER);
        assertEquals(64, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testInvalidateByProvider_doesNotAffectOtherProviders() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(o2, PROVIDER2, REQUESTER, diamond(), 20, true);
        manager.invalidateByProvider(PROVIDER);
        // PROVIDER2 reservation still active
        assertEquals(44, manager.effectiveAvailable(PROVIDER2, diamond(), 64));
    }

    // ===== releaseInFlight =====

    @Test
    void testReleaseInFlight_releasesTheWholeReservationWhenFullyResolved() {
        UUID orderId = UUID.randomUUID();
        ReservationId rid = manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.transition(rid, AllocationState.IN_TRANSIT);

        manager.releaseInFlight(orderId, diamond(), 10);

        assertEquals(64, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    @Test
    void testReleaseInFlight_keepsTheRemainderCommitted() {
        UUID orderId = UUID.randomUUID();
        ReservationId rid = manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.transition(rid, AllocationState.IN_TRANSIT);

        manager.releaseInFlight(orderId, diamond(), 4);

        assertEquals(58, manager.effectiveAvailable(PROVIDER, diamond(), 64),
                "Only the delivered portion is freed; the rest is still in flight");
    }

    @Test
    void testReleaseInFlight_leavesADifferentOrdersReservationAlone() {
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        ReservationId rid = manager.reserve(theirs, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.transition(rid, AllocationState.IN_TRANSIT);

        manager.releaseInFlight(mine, diamond(), 10);

        assertEquals(54, manager.effectiveAvailable(PROVIDER, diamond(), 64),
                "Two orders to the same requester must not resolve each other's reservations");
    }

    @Test
    void testReleaseInFlight_onlyMatchesInTransit() {
        UUID orderId = UUID.randomUUID();
        manager.reserve(orderId, PROVIDER, REQUESTER, diamond(), 10, true);
        // Still RESERVED: nothing has shipped, so nothing can have arrived.
        manager.releaseInFlight(orderId, diamond(), 10);

        assertEquals(54, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }

    // ===== getReservations =====

    @Test
    void testGetReservations_returnsAllForProvider() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);
        manager.reserve(o2, PROVIDER, REQUESTER2, emerald(), 5, false);
        manager.reserve(o1, PROVIDER2, REQUESTER, diamond(), 3, true); // different provider

        List<ItemReservation> result = manager.getReservations(PROVIDER);
        assertEquals(2, result.size());
    }

    // ===== merge =====

    @Test
    void testMerge_combinesReservationsFromBothManagers() {
        UUID o1 = UUID.randomUUID();
        UUID o2 = UUID.randomUUID();
        manager.reserve(o1, PROVIDER, REQUESTER, diamond(), 10, true);

        ReservationManager other = new ReservationManager();
        other.reserve(o2, PROVIDER, REQUESTER2, diamond(), 20, false);

        manager.merge(other);
        // 10 + 20 = 30 reserved against PROVIDER
        assertEquals(34, manager.effectiveAvailable(PROVIDER, diamond(), 64));
    }
}
