package com.logistics.pipe.network;

import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.network.RoutingPreference;
import com.logistics.core.lib.network.StrayClaim;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Re-homing a stray fluid packet onto a standing fluid order ({@link FluidOrderBook#claimStray}).
 *
 * <p>The fluid twin of {@link NetworkControllerStrayClaimTest}, with one deliberate difference: a
 * packet is indivisible on arrival and {@code resolveInTransit} rejects an over-report rather than
 * clamping it, so an order that cannot absorb the whole packet is not a candidate at all.
 */
@DisplayName("Stray fluid packet claim")
class FluidOrderBookStrayClaimTest extends MinecraftTestEnvironment {

    private static final BlockPos STRANDED = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER = new BlockPos(-4, 0, 0);
    // 2 hops from STRANDED along the pipe line, and the LESS positive position of the pair
    private static final BlockPos NEAR = new BlockPos(-2, 0, 0);
    // 6 hops from STRANDED, and the MORE positive position of the pair
    private static final BlockPos FAR = new BlockPos(6, 0, 0);

    private static final long PACKET_MB = 1000L;
    private static final Predicate<BlockPos> ANYWHERE = pos -> true;

    /** A straight run of pipes from x=-6 to x=6, so hop distance is real and measurable. */
    private static FluidOrderBook lineBook() {
        NetworkGraph graph = new NetworkGraph();
        for (int x = -6; x <= 6; x++) graph.addNode(new BlockPos(x, 0, 0));
        return new FluidOrderBook(graph::hopDistance);
    }

    // ==================== Choosing the order ====================

    @Test
    @DisplayName("a stray packet is claimed by the only order that wants it")
    void claimStray_takesTheMatchingOrder() {
        FluidOrderBook book = lineBook();
        UUID orderId = book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        StrayClaim claim = book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE);

        assertThat(claim).isNotNull();
        assertThat(claim.destination()).isEqualTo(NEAR);
        assertThat(claim.deliveryId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("an order for a different fluid never claims the packet")
    void claimStray_ignoresOtherFluids() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.LAVA, PACKET_MB, NEAR);

        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE)).isNull();
    }

    @Test
    @DisplayName("an order too small to absorb the whole packet is not a candidate")
    void claimStray_requiresTheOrderToAbsorbTheWholePacket() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.WATER, PACKET_MB - 1, NEAR);

        // The supplier acknowledges the packet's full mB on arrival and resolveInTransit rejects an
        // over-report, so claiming less than the packet carries would strand the order's accounting.
        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE)).isNull();
    }

    @Test
    @DisplayName("an unroutable requester is not a candidate")
    void claimStray_skipsUnroutableRequesters() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, pos -> !pos.equals(NEAR))).isNull();
    }

    @Test
    @DisplayName("the nearer requester wins even when position disagrees")
    void claimStray_prefersTheNearerRequester() {
        FluidOrderBook book = lineBook();
        assertThat(RoutingPreference.mostPositiveFirst(FAR, NEAR)).isLessThan(0);
        book.placeOrder(Fluids.WATER, PACKET_MB, FAR);
        book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE).destination()).isEqualTo(NEAR);
    }

    @Test
    @DisplayName("two orders from the same requester are claimed oldest first")
    void claimStray_fallsBackToFifo() {
        FluidOrderBook book = lineBook();
        UUID first = book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);
        book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE).deliveryId()).isEqualTo(first);
    }

    @Test
    @DisplayName("the winner is the same whether the network was merged or built whole")
    void claimStray_tieWinnerSurvivesAMerge() {
        FluidOrderBook whole = lineBook();
        whole.placeOrder(Fluids.WATER, PACKET_MB, NEAR);
        whole.placeOrder(Fluids.WATER, PACKET_MB, FAR);
        BlockPos expected = whole.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE).destination();

        NetworkGraph left = new NetworkGraph();
        NetworkGraph right = new NetworkGraph();
        for (int x = -6; x <= 0; x++) left.addNode(new BlockPos(x, 0, 0));
        for (int x = 1; x <= 6; x++) right.addNode(new BlockPos(x, 0, 0));
        FluidOrderBook leftHalf = new FluidOrderBook(left::hopDistance);
        FluidOrderBook rightHalf = new FluidOrderBook(right::hopDistance);
        // Reversed relative to the whole book: merging appends the other book's orders, so the merged
        // queue reads FAR-then-NEAR. Only a rule that ignores queue order can agree with above.
        leftHalf.placeOrder(Fluids.WATER, PACKET_MB, FAR);
        rightHalf.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        left.merge(right);
        leftHalf.merge(rightHalf);

        assertThat(leftHalf.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE).destination())
                .isEqualTo(expected);
    }

    // ==================== Accounting ====================

    @Test
    @DisplayName("claiming leaves the requester's outstanding mB alone, and delivery clears it")
    void claimStray_movesTheAmountFromQueuedToInFlight() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);

        StrayClaim claim = book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE);
        assertThat(book.getOrderedAmountFor(NEAR, Fluids.WATER))
                .as("the requester is still owed the fluid — it is in flight, not delivered")
                .isEqualTo(PACKET_MB);

        book.notifyDelivery(claim.deliveryId(), NEAR, Fluids.WATER, PACKET_MB);
        assertThat(book.getOrderedAmountFor(NEAR, Fluids.WATER))
                .as("the claimed delivery must be acknowledgeable against the claimed order")
                .isZero();
    }

    @Test
    @DisplayName("a packet smaller than the order leaves the remainder queued")
    void claimStray_partialClaimLeavesTheRemainder() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.WATER, 3 * PACKET_MB, NEAR);

        book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE);

        book.registerSupply(PROVIDER, Fluids.WATER, 10 * PACKET_MB, 1);
        assertThat(book.nextDispatchable().amountMb())
                .as("only the unclaimed remainder is still to be shipped")
                .isEqualTo(2 * PACKET_MB);
    }

    // ==================== Not racing the dispatch drain ====================

    @Test
    @DisplayName("a claim is refused while a dispatch drain is open")
    void claimStray_refusedDuringTheDispatchDrain() {
        FluidOrderBook book = lineBook();
        book.placeOrder(Fluids.WATER, PACKET_MB, NEAR);
        book.registerSupply(PROVIDER, Fluids.WATER, PACKET_MB, 1);

        book.beginDispatchDrain();
        // The order is committed to a provider but still queued at full size until recordDispatched.
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        assertThat(cmd).isNotNull();
        assertThat(book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE)).isNull();
        book.recordDispatched(cmd, cmd.amountMb());
        book.endDispatchDrain();

        assertThat(book.getOrderedAmountFor(NEAR, Fluids.WATER))
                .as("the shipped packet is the only thing in flight")
                .isEqualTo(PACKET_MB);
    }

    @Test
    @DisplayName("a claim and a provider dispatch never fill more than the order asked for")
    void claimStray_andDispatchShareTheSameOrder() {
        FluidOrderBook book = lineBook();
        UUID orderId = book.placeOrder(Fluids.WATER, 3 * PACKET_MB, NEAR);
        book.registerSupply(PROVIDER, Fluids.WATER, 10 * PACKET_MB, 1);

        book.claimStray(Fluids.WATER, PACKET_MB, STRANDED, ANYWHERE);
        FluidOrderBook.FluidDispatchCommand cmd = book.nextDispatchable();
        book.recordDispatched(cmd, cmd.amountMb());

        assertThat(cmd.orderId()).isEqualTo(orderId);
        assertThat(cmd.amountMb() + PACKET_MB)
                .as("the stray plus the shipment must total exactly what was ordered")
                .isEqualTo(3 * PACKET_MB);
        assertThat(book.nextDispatchable()).as("nothing is left to dispatch").isNull();
        assertThat(book.getOrderedAmountFor(NEAR, Fluids.WATER)).isEqualTo(3 * PACKET_MB);
    }
}
