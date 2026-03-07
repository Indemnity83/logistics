package com.logistics.pipe.network;

import com.logistics.test.MinecraftTestEnvironment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NetworkController}.
 *
 * <p>Replaces RequestMatcherTest. Tests key behaviors:
 * priority ordering, supply reservation, delivery accounting, order cancellation.
 */
class NetworkControllerTest extends MinecraftTestEnvironment {

    private NetworkController controller;
    private static final BlockPos PROVIDER1 = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(10, 0, 0);
    private static final BlockPos REQUESTER = new BlockPos(5, 0, 0);
    private static final BlockPos REQUESTER2 = new BlockPos(6, 0, 0);

    @BeforeEach
    void setUp() {
        controller = new NetworkController();
    }

    private ItemVariant diamond() {
        return ItemVariant.of(new ItemStack(Items.DIAMOND));
    }

    private ItemVariant emerald() {
        return ItemVariant.of(new ItemStack(Items.EMERALD));
    }

    // ===== Supply Registration =====

    @Test
    void testRegisterSupply_availableAmount() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        assertEquals(64L, controller.getAvailableAmount(diamond()));
    }

    @Test
    void testRegisterSupply_refresh_replacesOldEntries() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 32L), 1);
        assertEquals(32L, controller.getAvailableAmount(diamond()));
    }

    @Test
    void testRegisterSupply_emptyMapRemovesProvider() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.registerSupply(PROVIDER1, new HashMap<>(), 1);
        assertEquals(0L, controller.getAvailableAmount(diamond()));
    }

    @Test
    void testRemoveSupply() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.removeSupply(PROVIDER1);
        assertEquals(0L, controller.getAvailableAmount(diamond()));
    }

    // ===== Priority Dispatch =====

    @Test
    void testDispatch_realStockBeforeCrafter() {
        // Real stock (32) dispatched before crafter (0) even when crafter is also registered
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 32L), 1); // real stock
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 0L), 5);  // crafter

        UUID orderId = controller.placeOrder(diamond(), 16L, REQUESTER);
        assertNotNull(orderId);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(PROVIDER1, cmd.provider(), "Should dispatch from real stock first");
        assertEquals(REQUESTER, cmd.requester());
        assertEquals(16L, cmd.amount());
    }

    @Test
    void testDispatch_partialStockThenCraft() {
        // Provider has only 5, order needs 16 — dispatch 5 from stock, then 11 from crafter
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 5L), 1); // real stock (partial)
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 0L), 5); // crafter

        UUID orderId = controller.placeOrder(diamond(), 16L, REQUESTER);
        assertNotNull(orderId);

        // First dispatch: partial fill from real stock
        NetworkController.DispatchCommand cmd1 = controller.nextDispatchable();
        assertNotNull(cmd1);
        assertEquals(PROVIDER1, cmd1.provider(), "Should dispatch partial stock from provider first");
        assertEquals(5L, cmd1.amount(), "Should dispatch only what real stock has available");

        // Record the partial dispatch — order amount drops to 11
        controller.recordDispatched(orderId, 5L);

        // Second dispatch: remainder from crafter
        NetworkController.DispatchCommand cmd2 = controller.nextDispatchable();
        assertNotNull(cmd2);
        assertEquals(PROVIDER2, cmd2.provider(), "Should dispatch remainder from crafter");
        assertEquals(11L, cmd2.amount(), "Should dispatch exactly the remaining amount");
    }

    // ===== Supply Reservation =====

    @Test
    void testSupplyReservation_secondCallCannotMatchSameProvider() {
        // Only 16 available; two orders of 16 each — second should not match
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 16L), 1);

        controller.placeOrder(diamond(), 16L, REQUESTER);
        controller.placeOrder(diamond(), 16L, REQUESTER2);

        NetworkController.DispatchCommand first = controller.nextDispatchable();
        assertNotNull(first, "First order should be dispatchable");

        NetworkController.DispatchCommand second = controller.nextDispatchable();
        assertNull(second, "Second order should not dispatch — supply was reserved by first");
    }

    @Test
    void testMarkSupplyUnavailable_preventsRedispatch() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        UUID orderId = controller.placeOrder(diamond(), 16L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);

        // Provider fails to dispatch — mark unavailable
        controller.markSupplyUnavailable(PROVIDER1);

        // Should not dispatch to same provider again this tick
        NetworkController.DispatchCommand cmd2 = controller.nextDispatchable();
        assertNull(cmd2, "Should not re-dispatch to unavailable provider");
    }

    // ===== recordDispatched =====

    @Test
    void testRecordDispatched_fullAmount_removesOrder() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        UUID orderId = controller.placeOrder(diamond(), 32L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 32L);

        // No more orders
        assertNull(controller.nextDispatchable());
    }

    @Test
    void testRecordDispatched_partialAmount_reducesOrder() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        UUID orderId = controller.placeOrder(diamond(), 32L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 16L); // only shipped half

        // Order should remain with reduced amount
        // Re-register supply (simulate provider scanning after dispatch)
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 48L), 1);
        NetworkController.DispatchCommand cmd2 = controller.nextDispatchable();
        assertNotNull(cmd2, "Remaining order should still be dispatchable");
        assertEquals(16L, cmd2.amount(), "Remaining amount should be 16");
    }

    // ===== notifyDelivery =====

    @Test
    void testNotifyDelivery_decrementsOrderedForRequester() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.placeOrder(diamond(), 32L, REQUESTER);

        assertEquals(32L, controller.getOrderedAmountFor(REQUESTER, diamond()));

        controller.notifyDelivery(REQUESTER, diamond(), 32L);

        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, diamond()));
    }

    @Test
    void testNotifyDelivery_partialDelivery() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.placeOrder(diamond(), 32L, REQUESTER);

        controller.notifyDelivery(REQUESTER, diamond(), 16L);

        assertEquals(16L, controller.getOrderedAmountFor(REQUESTER, diamond()));
    }

    // ===== cancelOrder =====

    @Test
    void testCancelOrder_removesFromQueueAndDecrementsOrdered() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        UUID orderId = controller.placeOrder(diamond(), 32L, REQUESTER);

        assertEquals(32L, controller.getOrderedAmountFor(REQUESTER, diamond()));

        controller.cancelOrder(orderId);

        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, diamond()));
        assertNull(controller.nextDispatchable(), "Cancelled order should not dispatch");
    }

    // ===== cancelOrdersFor =====

    @Test
    void testCancelOrdersFor_removesAllOrdersForRequester() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L, emerald(), 32L), 1);
        controller.placeOrder(diamond(), 16L, REQUESTER);
        controller.placeOrder(emerald(), 8L, REQUESTER);

        controller.cancelOrdersFor(REQUESTER);

        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, diamond()));
        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, emerald()));
        assertNull(controller.nextDispatchable());
    }

    @Test
    void testCancelOrdersFor_doesNotAffectOtherRequesters() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.placeOrder(diamond(), 16L, REQUESTER);
        controller.placeOrder(diamond(), 8L, REQUESTER2);

        controller.cancelOrdersFor(REQUESTER);

        assertEquals(8L, controller.getOrderedAmountFor(REQUESTER2, diamond()));
    }

    // ===== merge =====

    @Test
    void testMerge_combinesSupplyAndOrders() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        controller.placeOrder(diamond(), 16L, REQUESTER);

        NetworkController other = new NetworkController();
        other.registerSupply(PROVIDER2, Map.of(emerald(), 32L), 1);
        other.placeOrder(emerald(), 8L, REQUESTER2);

        controller.merge(other);

        assertEquals(64L, controller.getAvailableAmount(diamond()));
        assertEquals(32L, controller.getAvailableAmount(emerald()));
        assertEquals(16L, controller.getOrderedAmountFor(REQUESTER, diamond()));
        assertEquals(8L, controller.getOrderedAmountFor(REQUESTER2, emerald()));
    }

    // ===== getAllAvailableItems =====

    @Test
    void testGetAllAvailableItems() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 32L, emerald(), 16L), 1);

        Map<ItemStack, Long> items = controller.getAllAvailableItems();
        assertEquals(2, items.size());
    }
}
