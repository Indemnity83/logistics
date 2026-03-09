package com.logistics.pipe.network;

import com.logistics.test.MinecraftTestEnvironment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReconciliationService}.
 */
class ReconciliationServiceTest extends MinecraftTestEnvironment {

    private ReconciliationService service;
    private NetworkController controller;

    private static final BlockPos PROVIDER1 = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(10, 0, 0);
    private static final BlockPos DESTINATION = new BlockPos(5, 0, 0);

    @BeforeEach
    void setUp() {
        service = new ReconciliationService();
        controller = new NetworkController();
    }

    private ItemVariant diamond() {
        return ItemVariant.of(new ItemStack(Items.DIAMOND));
    }

    private NetworkJob activeJob(ItemVariant item, long amount, FulfillmentMode mode) {
        NetworkJob job = new NetworkJob(UUID.randomUUID(), item, amount, amount, mode, DESTINATION);
        job.transitionTo(JobState.ACTIVE);
        return job;
    }

    // ===== reconcile: no loss =====

    @Test
    void testReconcile_supplyStillAvailable_noLoss() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        ReconciliationResult result = service.reconcile(job, controller);

        assertFalse(result.hasLoss());
        assertEquals(0, result.amountLost());
    }

    @Test
    void testReconcile_zeroOutstanding_noLoss() {
        // A job that has already been fully delivered (outstanding = 0)
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);
        job.recordDelivery(16); // outstanding → 0

        ReconciliationResult result = service.reconcile(job, controller);

        assertFalse(result.hasLoss());
    }

    // ===== reconcile: supply dropped =====

    @Test
    void testReconcile_supplyGone_fullLoss() {
        // No supply registered → planner can source 0 → full loss
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        ReconciliationResult result = service.reconcile(job, controller);

        assertTrue(result.hasLoss());
        assertEquals(16, result.amountLost());
    }

    @Test
    void testReconcile_partialSupply_partialLoss() {
        // Only 5 available, job needs 16 → 11 lost
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 5L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        ReconciliationResult result = service.reconcile(job, controller);

        assertTrue(result.hasLoss());
        assertEquals(11, result.amountLost());
    }

    @Test
    void testReconcile_exactSupply_noLoss() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 16L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        ReconciliationResult result = service.reconcile(job, controller);

        assertFalse(result.hasLoss());
    }

    // ===== replan: alternative supply found =====

    @Test
    void testReplan_alternativeSupplyAvailable_returnsNewPlan() {
        // PROVIDER2 has stock that can cover the missing amount
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 64L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        Optional<FulfillmentPlan> result = service.replan(job, 10, controller);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().plannedAmount());
    }

    @Test
    void testReplan_noAlternativeSupply_returnsEmpty() {
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        Optional<FulfillmentPlan> result = service.replan(job, 10, controller);

        assertFalse(result.isPresent());
    }

    // ===== replan: FULL mode respects completeness =====

    @Test
    void testReplan_fullMode_onlyPartialAvailable_returnsEmpty() {
        // 5 available, but missing 10 — FULL mode requires all 10
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 5L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.FULL);

        Optional<FulfillmentPlan> result = service.replan(job, 10, controller);

        assertFalse(result.isPresent(), "FULL mode should not return partial plan");
    }

    @Test
    void testReplan_fullMode_exactSupplyAvailable_returnsplan() {
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 10L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.FULL);

        Optional<FulfillmentPlan> result = service.replan(job, 10, controller);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().plannedAmount());
    }

    @Test
    void testReplan_partialMode_partialAvailable_returnsPartialPlan() {
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 5L), 1);
        NetworkJob job = activeJob(diamond(), 16, FulfillmentMode.PARTIAL);

        Optional<FulfillmentPlan> result = service.replan(job, 10, controller);

        assertTrue(result.isPresent());
        assertEquals(5, result.get().plannedAmount());
    }

    // ===== ReconciliationResult =====

    @Test
    void testNone_hasNoLoss() {
        ReconciliationResult none = ReconciliationResult.none();
        assertFalse(none.hasLoss());
        assertEquals(0, none.amountLost());
    }

    @Test
    void testHasLoss_whenAmountLostPositive() {
        ReconciliationResult result = new ReconciliationResult(5);
        assertTrue(result.hasLoss());
        assertEquals(5, result.amountLost());
    }
}
