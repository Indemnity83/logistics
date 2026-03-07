package com.logistics.pipe.network;

import com.logistics.test.MinecraftTestEnvironment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final BlockPos CRAFTER_POS = new BlockPos(20, 0, 0);
    private static final BlockPos CRAFTER_POS2 = new BlockPos(21, 0, 0);
    private static final BlockPos CRAFTER_POS3 = new BlockPos(22, 0, 0);
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

    private ItemVariant oakLog() {
        return ItemVariant.of(new ItemStack(Items.OAK_LOG));
    }

    private ItemVariant oakPlanks() {
        return ItemVariant.of(new ItemStack(Items.OAK_PLANKS));
    }

    private ItemVariant stick() {
        return ItemVariant.of(new ItemStack(Items.STICK));
    }

    private ItemVariant ironIngot() {
        return ItemVariant.of(new ItemStack(Items.IRON_INGOT));
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

    // ===== Ingredient chain validation =====

    @Test
    void testIngredientChainFail_cancelsCrafterOrder() {
        // Crafter can make planks but needs logs; 0 logs available
        controller.registerSupply(CRAFTER_POS, Map.of(oakPlanks(), 0L), 5);
        controller.registerProviderCheck(CRAFTER_POS, (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount));

        List<UUID> failedOrders = new ArrayList<>();
        controller.setOrderFailureListener((orderId, req, item, amt, missing) ->
                failedOrders.add(orderId));

        UUID orderId = controller.placeOrder(oakPlanks(), 8L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNull(cmd, "Order with missing ingredients should not dispatch");
        assertTrue(failedOrders.contains(orderId), "Failure listener should be called");
        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, oakPlanks()),
                "orderedForRequester should be decremented on failure");
    }

    @Test
    void testIngredientChainOk_dispatchesCrafter() {
        // Crafter can make planks; logs are in stock
        controller.registerSupply(CRAFTER_POS, Map.of(oakPlanks(), 0L), 5);
        controller.registerSupply(PROVIDER1, Map.of(oakLog(), 10L), 1);
        controller.registerProviderCheck(CRAFTER_POS, (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount));

        controller.placeOrder(oakPlanks(), 4L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd, "Order should dispatch when ingredients are available");
        assertEquals(CRAFTER_POS, cmd.provider());
        assertEquals(4L, cmd.amount());
    }

    @Test
    void testPartialFillWithCrafterFail_preventsPartialDispatch() {
        // 20 planks in stock + crafter (needs logs), need 30, 0 logs → full order cancelled
        controller.registerSupply(PROVIDER1, Map.of(oakPlanks(), 20L), 1);
        controller.registerSupply(CRAFTER_POS, Map.of(oakPlanks(), 0L), 5);
        controller.registerProviderCheck(CRAFTER_POS, (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount)); // no logs registered

        List<UUID> failedOrders = new ArrayList<>();
        controller.setOrderFailureListener((orderId, req, item, amt, missing) ->
                failedOrders.add(orderId));

        UUID orderId = controller.placeOrder(oakPlanks(), 30L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNull(cmd, "Partial dispatch should be blocked when crafter chain will fail");
        assertTrue(failedOrders.contains(orderId), "Failure listener should be called");
        // Ordered amount should be decremented (order cancelled, not just deferred)
        assertEquals(0L, controller.getOrderedAmountFor(REQUESTER, oakPlanks()),
                "orderedForRequester should be decremented when order is cancelled");
    }

    @Test
    void testPartialFillNoCrafter_stillDispatchesPartial() {
        // 15 planks in stock, need 16, no crafter → dispatch partial 15 (no pre-check)
        controller.registerSupply(PROVIDER1, Map.of(oakPlanks(), 15L), 1);

        controller.placeOrder(oakPlanks(), 16L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd, "Partial dispatch should proceed when there is no crafter");
        assertEquals(PROVIDER1, cmd.provider());
        assertEquals(15L, cmd.amount());
    }

    @Test
    void testNoCheckRegistered_crafterDispatchesOptimistically() {
        // Crafter with available=0 but no ProviderCanFulfill registered → dispatch proceeds
        controller.registerSupply(CRAFTER_POS, Map.of(oakPlanks(), 0L), 5);
        // No registerProviderCheck call

        controller.placeOrder(oakPlanks(), 8L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd, "Crafter without a registered check should dispatch optimistically");
        assertEquals(CRAFTER_POS, cmd.provider());
    }

    @Test
    void testSharedClaimed_siblingBranchesSeeSameStock() {
        // Chain: G crafter needs 1 A + 1 B; A crafter needs 1 X; B crafter needs 1 X; only 1 X in stock
        // Validation of G should fail because X cannot satisfy both A and B
        ItemVariant x = ironIngot();
        ItemVariant a = oakPlanks();
        ItemVariant b = stick();
        ItemVariant g = diamond();

        controller.registerSupply(PROVIDER1, Map.of(x, 1L), 1);
        controller.registerSupply(CRAFTER_POS, Map.of(a, 0L), 5);
        controller.registerSupply(CRAFTER_POS2, Map.of(b, 0L), 5);
        controller.registerSupply(CRAFTER_POS3, Map.of(g, 0L), 5);

        // A crafter: 1 X → 1 A
        controller.registerProviderCheck(CRAFTER_POS, (amount, checker) ->
                checker.check(x.toStack(1), amount));
        // B crafter: 1 X → 1 B
        controller.registerProviderCheck(CRAFTER_POS2, (amount, checker) ->
                checker.check(x.toStack(1), amount));
        // G crafter: 1 A + 1 B → 1 G
        controller.registerProviderCheck(CRAFTER_POS3, (amount, checker) -> {
            List<ItemVariant> missing = new ArrayList<>();
            missing.addAll(checker.check(a.toStack(1), amount)); // uses 1 X
            missing.addAll(checker.check(b.toStack(1), amount)); // needs another X — none left
            return missing;
        });

        controller.placeOrder(g, 1L, REQUESTER);

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNull(cmd, "G order should fail: only 1 X available but both A and B need it");
    }

    @Test
    void testDeepChain_failsWithRootCause() {
        // Chain: planks crafter (needs logs) → logs not available
        // Result: order fails and reported missing item is the root (logs), not the intermediate
        controller.registerSupply(CRAFTER_POS, Map.of(oakPlanks(), 0L), 5);
        controller.registerProviderCheck(CRAFTER_POS, (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount));

        List<ItemVariant> capturedMissing = new ArrayList<>();
        controller.setOrderFailureListener((orderId, req, item, amt, missing) ->
                capturedMissing.addAll(missing));

        controller.placeOrder(oakPlanks(), 4L, REQUESTER);
        controller.nextDispatchable();

        assertFalse(capturedMissing.isEmpty(), "Missing list should be reported");
        assertTrue(capturedMissing.stream().anyMatch(v -> v.getItem() == Items.OAK_LOG),
                "Missing item should be the root ingredient (oak log), got: " + capturedMissing);
    }
}
