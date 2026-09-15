package com.logistics.pipe.network;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.NetworkGraph;
import com.logistics.core.lib.network.RoutingPreference;
import com.logistics.core.lib.network.StrayClaim;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Re-homing a stray item onto a standing order ({@link NetworkController#claimStray}).
 *
 * <p>A packet that loses its destination can only be recovered through the order book: suppliers and
 * requesters place orders but never register as sinks, so sink resolution cannot see them. These
 * tests pin the choice of order, the accounting it moves, and the guarantee that the claim and the
 * dispatch drain can never settle the same queued amount twice.
 */
@DisplayName("Stray item claim")
class NetworkControllerStrayClaimTest extends MinecraftTestEnvironment {

    private static final BlockPos STRANDED = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER = new BlockPos(-4, 0, 0);
    // 2 hops from STRANDED along the pipe line, and the LESS positive position of the pair
    private static final BlockPos NEAR = new BlockPos(-2, 0, 0);
    // 6 hops from STRANDED, and the MORE positive position of the pair
    private static final BlockPos FAR = new BlockPos(6, 0, 0);

    private static final Predicate<BlockPos> ANYWHERE = pos -> true;

    private static IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private static IItemKey emerald() {
        return new TestItemKey(Items.EMERALD);
    }

    /** A straight run of pipes from x=-6 to x=6, so hop distance is real and measurable. */
    private static NetworkController lineController() {
        NetworkGraph graph = new NetworkGraph();
        for (int x = -6; x <= 6; x++) graph.addNode(new BlockPos(x, 0, 0));
        return new NetworkController(graph::hopDistance);
    }

    // ==================== Choosing the order ====================

    @Test
    @DisplayName("a stray stack is claimed by the only order that wants it")
    void claimStray_takesTheMatchingOrder() {
        NetworkController controller = lineController();
        UUID orderId = controller.placeOrder(diamond(), 16L, NEAR);

        StrayClaim claim = controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE);

        assertThat(claim).isNotNull();
        assertThat(claim.destination()).isEqualTo(NEAR);
        assertThat(claim.deliveryId()).isEqualTo(orderId);
        assertThat(controller.hasOrder(orderId))
                .as("a fully claimed order is no longer queued for a provider to fill")
                .isFalse();
    }

    @Test
    @DisplayName("an order for a different item never claims the stack")
    void claimStray_ignoresOtherItems() {
        NetworkController controller = lineController();
        controller.placeOrder(emerald(), 16L, NEAR);

        assertThat(controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE)).isNull();
    }

    @Test
    @DisplayName("an unroutable requester is not a candidate")
    void claimStray_skipsUnroutableRequesters() {
        NetworkController controller = lineController();
        controller.placeOrder(diamond(), 16L, NEAR);

        assertThat(controller.claimStray(diamond(), 16L, STRANDED, pos -> !pos.equals(NEAR))).isNull();
    }

    @Test
    @DisplayName("the nearer requester wins even when position disagrees")
    void claimStray_prefersTheNearerRequester() {
        NetworkController controller = lineController();
        // FAR is the more positive position, so only routed distance can hand the stack to NEAR —
        // and FAR is ordered first, so only distance can beat FIFO either.
        assertThat(RoutingPreference.mostPositiveFirst(FAR, NEAR)).isLessThan(0);
        controller.placeOrder(diamond(), 16L, FAR);
        controller.placeOrder(diamond(), 16L, NEAR);

        assertThat(controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE).destination()).isEqualTo(NEAR);
    }

    @Test
    @DisplayName("distance is measured from where the stack is stranded")
    void claimStray_measuresFromTheStrandedPosition() {
        NetworkController controller = lineController();
        controller.placeOrder(diamond(), 16L, NEAR);
        controller.placeOrder(diamond(), 16L, FAR);

        // From the far end of the line the ranking flips: FAR is 1 hop away, NEAR is 7.
        assertThat(controller.claimStray(diamond(), 16L, new BlockPos(5, 0, 0), ANYWHERE).destination())
                .isEqualTo(FAR);
    }

    @Test
    @DisplayName("two orders from the same requester are claimed oldest first")
    void claimStray_fallsBackToFifo() {
        NetworkController controller = lineController();
        UUID first = controller.placeOrder(diamond(), 16L, NEAR);
        controller.placeOrder(diamond(), 16L, NEAR);

        assertThat(controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE).deliveryId()).isEqualTo(first);
    }

    @Test
    @DisplayName("the winner is the same whether the network was merged or built whole")
    void claimStray_tieWinnerSurvivesAMerge() {
        NetworkController whole = lineController();
        whole.placeOrder(diamond(), 16L, NEAR);
        whole.placeOrder(diamond(), 16L, FAR);
        BlockPos expected = whole.claimStray(diamond(), 16L, STRANDED, ANYWHERE).destination();

        // Same orders, but placed on two controllers that then merge — the case where insertion and
        // hash-iteration order differ from the built-whole controller.
        NetworkGraph left = new NetworkGraph();
        NetworkGraph right = new NetworkGraph();
        for (int x = -6; x <= 0; x++) left.addNode(new BlockPos(x, 0, 0));
        for (int x = 1; x <= 6; x++) right.addNode(new BlockPos(x, 0, 0));
        NetworkController leftHalf = new NetworkController(left::hopDistance);
        NetworkController rightHalf = new NetworkController(right::hopDistance);
        // Reversed relative to the whole controller: merging appends the other book's orders, so the
        // merged queue reads FAR-then-NEAR. Only a rule that ignores queue order can agree with above.
        leftHalf.placeOrder(diamond(), 16L, FAR);
        rightHalf.placeOrder(diamond(), 16L, NEAR);

        left.merge(right);
        leftHalf.merge(rightHalf);

        assertThat(leftHalf.claimStray(diamond(), 16L, STRANDED, ANYWHERE).destination()).isEqualTo(expected);
    }

    // ==================== Accounting ====================

    @Test
    @DisplayName("claiming leaves the requester's outstanding total alone, and delivery clears it")
    void claimStray_movesTheAmountFromQueuedToInFlight() {
        NetworkController controller = lineController();
        controller.placeOrder(diamond(), 16L, NEAR);

        StrayClaim claim = controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE);
        assertThat(controller.getOrderedAmountFor(NEAR, diamond()))
                .as("the requester is still owed the items — they are in flight, not delivered")
                .isEqualTo(16L);

        controller.notifyDelivery(claim.deliveryId(), NEAR, diamond(), 16L);
        assertThat(controller.getOrderedAmountFor(NEAR, diamond()))
                .as("the claimed delivery must be acknowledgeable against the claimed order")
                .isZero();
    }

    @Test
    @DisplayName("a stray smaller than the order leaves the remainder queued")
    void claimStray_partialClaimLeavesTheRemainder() {
        NetworkController controller = lineController();
        UUID orderId = controller.placeOrder(diamond(), 16L, NEAR);

        controller.claimStray(diamond(), 6L, STRANDED, ANYWHERE);

        assertThat(controller.hasOrder(orderId)).isTrue();
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        assertThat(controller.nextDispatchable().amount())
                .as("only the unclaimed remainder is still to be shipped")
                .isEqualTo(10L);
    }

    @Test
    @DisplayName("a stray larger than the order is claimed only for what the order wants")
    void claimStray_neverClaimsMoreThanTheOrder() {
        NetworkController controller = lineController();
        controller.placeOrder(diamond(), 4L, NEAR);

        StrayClaim claim = controller.claimStray(diamond(), 64L, STRANDED, ANYWHERE);

        // The surplus is clamped on arrival rather than over-releasing the requester's accounting.
        controller.notifyDelivery(claim.deliveryId(), NEAR, diamond(), 64L);
        assertThat(controller.getOrderedAmountFor(NEAR, diamond())).isZero();
    }

    // ==================== Not racing the dispatch drain ====================

    @Test
    @DisplayName("a claim is refused while a dispatch drain is open")
    void claimStray_refusedDuringTheDispatchDrain() {
        NetworkController controller = lineController();
        controller.placeOrder(diamond(), 16L, NEAR);
        controller.registerSupply(PROVIDER, Map.of(diamond(), 16L), 1);

        controller.beginDispatchDrain();
        // The order has been committed to a provider but recordDispatched has not run yet: this is
        // the only window in which a claim could settle an amount the provider is already shipping.
        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertThat(cmd).isNotNull();
        assertThat(controller.claimStray(diamond(), 16L, STRANDED, ANYWHERE)).isNull();
        controller.recordDispatched(cmd.orderId(), cmd.amount());
        controller.endDispatchDrain();

        assertThat(controller.getOrderedAmountFor(NEAR, diamond()))
                .as("the shipped stack is the only thing in flight")
                .isEqualTo(16L);
    }

    @Test
    @DisplayName("a claim and a provider dispatch never fill more than the order asked for")
    void claimStray_andDispatchShareTheSameOrder() {
        NetworkController controller = lineController();
        UUID orderId = controller.placeOrder(diamond(), 16L, NEAR, FulfillmentMode.PARTIAL);
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);

        controller.claimStray(diamond(), 6L, STRANDED, ANYWHERE);
        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        controller.recordDispatched(cmd.orderId(), cmd.amount());

        assertThat(cmd.orderId()).isEqualTo(orderId);
        assertThat(cmd.amount() + 6L)
                .as("the stray plus the shipment must total exactly what was ordered")
                .isEqualTo(16L);
        assertThat(controller.hasOrder(orderId)).isFalse();
        assertThat(controller.getOrderedAmountFor(NEAR, diamond())).isEqualTo(16L);
    }
}
