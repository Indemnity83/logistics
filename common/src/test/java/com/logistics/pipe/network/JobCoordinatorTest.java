package com.logistics.pipe.network;

import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.ItemRequest;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JobCoordinator}.
 */
class JobCoordinatorTest extends MinecraftTestEnvironment {

    private NetworkController controller;
    private JobCoordinator coordinator;

    private static final BlockPos PROVIDER = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(1, 0, 0);
    private static final BlockPos DESTINATION = new BlockPos(5, 0, 0);
    private static final BlockPos DESTINATION2 = new BlockPos(6, 0, 0);

    @BeforeEach
    void setUp() {
        controller = new NetworkController();
        coordinator = new JobCoordinator(controller);
        // Wire failure listener so coordinator receives order failure callbacks
        controller.setOrderFailureListener(coordinator);
    }

    private IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private IItemKey emerald() {
        return new TestItemKey(Items.EMERALD);
    }

    private ItemRequest partialRequest(IItemKey item, long amount, BlockPos dest) {
        return new ItemRequest(item, amount, dest, FulfillmentMode.PARTIAL);
    }

    private ItemRequest fullRequest(IItemKey item, long amount, BlockPos dest) {
        return new ItemRequest(item, amount, dest, FulfillmentMode.FULL);
    }

    // ===== submit =====

    @Test
    void testSubmit_createsActiveJob() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);

        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        assertNotNull(job);
        assertEquals(JobState.ACTIVE, job.state());
        assertEquals(diamond(), job.item());
        assertEquals(16, job.requestedAmount());
        assertEquals(DESTINATION, job.destination());
    }

    @Test
    void testSubmit_jobAppearsInActiveJobs() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);

        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        assertTrue(coordinator.activeJobs().contains(job));
    }

    @Test
    void testSubmit_placesOrderInController() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);

        coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        // Controller should have an order → should be dispatchable
        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        assertEquals(16, cmd.amount());
    }

    @Test
    void testSubmit_workOrdersCreatedFromPlan() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);

        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        // Should have at least an ExtractWorkOrder and a DeliverWorkOrder
        assertFalse(job.workOrders().isEmpty());
        boolean hasExtract = job.workOrders().stream().anyMatch(w -> w instanceof WorkOrder.ExtractWorkOrder);
        boolean hasDeliver = job.workOrders().stream().anyMatch(w -> w instanceof WorkOrder.DeliverWorkOrder);
        assertTrue(hasExtract, "Job should have an ExtractWorkOrder");
        assertTrue(hasDeliver, "Job should have a DeliverWorkOrder");
    }

    // ===== onDelivery =====

    @Test
    void testOnDelivery_partialDelivery_jobStillActive() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        coordinator.onDelivery(DESTINATION, diamond(), 8);

        assertEquals(JobState.DELIVERING, job.state());
        assertEquals(8, job.deliveredAmount());
        assertEquals(8, job.outstanding());
    }

    @Test
    void testOnDelivery_fullDelivery_jobComplete() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        coordinator.onDelivery(DESTINATION, diamond(), 16);

        assertEquals(JobState.COMPLETE, job.state());
        assertEquals(16, job.deliveredAmount());
        assertEquals(0, job.outstanding());
    }

    @Test
    void testOnDeliveryFailed_retriesWithoutCountingDelivery() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 16);

        UUID replacementOrderId = controller.notifyDeliveryFailed(cmd.orderId(), DESTINATION, diamond(), 16);
        coordinator.onDeliveryFailed(cmd.orderId(), replacementOrderId);

        assertEquals(JobState.ACTIVE, job.state());
        assertEquals(0, job.deliveredAmount());
        assertEquals(16, job.outstanding());
        assertTrue(coordinator.activeJobs().contains(job));

        controller.registerSupply(PROVIDER2, Map.of(diamond(), 16L), 1);
        NetworkController.DispatchCommand retry = controller.nextDispatchable();
        assertNotNull(retry, "Failed delivery should create a replacement order for fresh supply");
        assertEquals(replacementOrderId, retry.orderId());
        assertEquals(16, retry.amount());
    }

    @Test
    void testOnDeliveryFailed_afterPartialDelivery_retriesOnlyRemainder() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 32, DESTINATION));

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 32);

        controller.notifyDelivery(cmd.orderId(), DESTINATION, diamond(), 12);
        coordinator.onDelivery(DESTINATION, diamond(), 12);
        UUID replacementOrderId = controller.notifyDeliveryFailed(cmd.orderId(), DESTINATION, diamond(), 20);
        coordinator.onDeliveryFailed(cmd.orderId(), replacementOrderId);

        assertEquals(JobState.ACTIVE, job.state());
        assertEquals(12, job.deliveredAmount());
        assertEquals(20, job.outstanding());
        assertTrue(coordinator.activeJobs().contains(job));

        controller.registerSupply(PROVIDER2, Map.of(diamond(), 20L), 1);
        NetworkController.DispatchCommand retry = controller.nextDispatchable();
        assertNotNull(retry, "Failed remainder should create a replacement order");
        assertEquals(replacementOrderId, retry.orderId());
        assertEquals(20, retry.amount());
    }

    @Test
    void testOnDeliveryFailed_dropsStaleOrderIndex_soLateFailureOfOldOrderIsIgnored() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 16);
        UUID replacementOrderId = controller.notifyDeliveryFailed(cmd.orderId(), DESTINATION, diamond(), 16);
        coordinator.onDeliveryFailed(cmd.orderId(), replacementOrderId);
        assertEquals(JobState.ACTIVE, job.state());

        // A late failure callback for the superseded order must not touch a job that is actively
        // retrying — onDeliveryFailed drops the old order's index entry, so this resolves to no job.
        coordinator.onOrderFailed(cmd.orderId(), DESTINATION, diamond(), 16, List.of());

        assertEquals(JobState.ACTIVE, job.state());
        assertTrue(coordinator.activeJobs().contains(job));
    }

    @Test
    void testOnDelivery_doesNotAffectDifferentItem() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L, emerald(), 32L), 1);
        NetworkJob diamondJob = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        coordinator.onDelivery(DESTINATION, emerald(), 8);

        assertEquals(JobState.ACTIVE, diamondJob.state());
        assertEquals(0, diamondJob.deliveredAmount());
    }

    @Test
    void testOnDelivery_doesNotAffectDifferentDestination() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        coordinator.onDelivery(DESTINATION2, diamond(), 16);

        assertEquals(JobState.ACTIVE, job.state());
        assertEquals(0, job.deliveredAmount());
    }

    // ===== onOrderFailed =====

    @Test
    void testOnOrderFailed_transitionsJobToFailed() {
        // Crafter with no ingredients → order will fail
        controller.registerSupply(new BlockPos(20, 0, 0), Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(new BlockPos(20, 0, 0), (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount)); // needs logs, none registered

        NetworkJob job = coordinator.submit(partialRequest(diamond(), 8, DESTINATION));

        // Trigger dispatch attempt → ingredient check fails → onOrderFailed is called
        controller.nextDispatchable();

        assertEquals(JobState.FAILED, job.state());
    }

    @Test
    void testOnOrderFailed_jobRemovedFromActiveJobs() {
        controller.registerSupply(new BlockPos(20, 0, 0), Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(new BlockPos(20, 0, 0), (amount, checker) ->
                checker.check(new ItemStack(Items.OAK_LOG), amount));

        NetworkJob job = coordinator.submit(partialRequest(diamond(), 8, DESTINATION));
        controller.nextDispatchable();

        assertFalse(coordinator.activeJobs().contains(job));
    }

    // ===== purgeTerminal =====

    @Test
    void testPurgeTerminal_removesCompletedJobs() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));
        coordinator.onDelivery(DESTINATION, diamond(), 16); // → COMPLETE

        coordinator.purgeTerminal();

        assertFalse(coordinator.allJobs().contains(job));
    }

    @Test
    void testPurgeTerminal_keepsActiveJobs() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        coordinator.purgeTerminal();

        assertTrue(coordinator.allJobs().contains(job));
    }

    // ===== merge =====

    @Test
    void testMerge_combinesJobsFromBothCoordinators() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L, emerald(), 32L), 1);

        NetworkJob job1 = coordinator.submit(partialRequest(diamond(), 8, DESTINATION));

        NetworkController otherController = new NetworkController();
        otherController.registerSupply(PROVIDER, Map.of(emerald(), 32L), 1);
        JobCoordinator other = new JobCoordinator(otherController);
        NetworkJob job2 = other.submit(partialRequest(emerald(), 4, DESTINATION2));

        coordinator.merge(other);

        assertTrue(coordinator.allJobs().contains(job1));
        assertTrue(coordinator.allJobs().contains(job2));
    }

    // ===== tick (reconciliation) =====

    @Test
    void testTick_doesNotRunBeforeInterval() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        // Tick < 100 times — reconciliation should not run
        for (int i = 0; i < 99; i++) {
            coordinator.tick(controller);
        }

        // Job still active, not modified by reconciliation
        assertEquals(JobState.ACTIVE, job.state());
    }

    @Test
    void testTick_runsReconciliationAfterInterval() {
        // Register supply, submit job, then remove supply to simulate stock loss
        controller.registerSupply(PROVIDER, Map.of(diamond(), 16L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));
        // Remove supply while order is still pending
        controller.removeSupply(PROVIDER);

        // Tick 100 times to trigger reconciliation
        for (int i = 0; i < 100; i++) {
            coordinator.tick(controller);
        }

        // After reconciliation detects total loss with no alternative supply,
        // job.recordInvalidation() is called, eventually marking job FAILED if nothing delivered
        // At minimum, outstanding should be reduced by the loss amount
        assertTrue(job.invalidatedAmount() > 0 || job.state() == JobState.FAILED,
                "Job should have recorded invalidation after supply disappeared, state=" + job.state());
    }

    @Test
    void testTick_replanKeepsTheStillSourceableRemainder() {
        // 64 across two chests; one is broken mid-order, so 24 is lost and 40 is still there.
        controller.registerSupply(PROVIDER, Map.of(diamond(), 40L), 1);
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 24L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 64, DESTINATION));
        assertEquals(64, job.outstanding());

        controller.removeSupply(PROVIDER2);
        for (int i = 0; i < 100; i++) {
            coordinator.tick(controller);
        }

        assertEquals(40, controller.getOrderedAmountFor(DESTINATION, diamond()),
                "The replacement order must cover what can still be sourced, not only what was lost");
        assertEquals(40, job.outstanding(),
                "The job's planned amount must follow the replan, or it can never finish");
    }

    @Test
    void testTick_replannedJobCanStillReachATerminalState() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 40L), 1);
        controller.registerSupply(PROVIDER2, Map.of(diamond(), 24L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 64, DESTINATION));

        controller.removeSupply(PROVIDER2);
        for (int i = 0; i < 100; i++) {
            coordinator.tick(controller);
        }

        coordinator.onDelivery(DESTINATION, diamond(), 40);

        assertEquals(JobState.COMPLETE, job.state(),
                "Delivering everything still sourceable must finish the job");
    }

    @Test
    void testPurgeTerminal_cancelsTheOrderItLeavesBehind() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 16L), 1);
        coordinator.submit(partialRequest(diamond(), 16, DESTINATION));
        assertEquals(16, controller.getOrderedAmountFor(DESTINATION, diamond()));

        // Supply disappears with the order still queued: the job settles terminal while the
        // controller is still holding its order.
        controller.removeSupply(PROVIDER);
        for (int i = 0; i < 200; i++) {
            coordinator.tick(controller);
        }

        assertEquals(0, controller.getOrderedAmountFor(DESTINATION, diamond()),
                "A purged job must not leave a standing order behind — requesters read that figure "
                        + "to decide not to re-order");
    }

    @Test
    void testTick_skipsJobWithDispatchedOrder() {
        controller.registerSupply(PROVIDER, Map.of(diamond(), 64L), 1);
        NetworkJob job = coordinator.submit(partialRequest(diamond(), 16, DESTINATION));

        // Dispatch the order (removes it from queue) and then remove supply
        NetworkController.DispatchCommand cmd = controller.nextDispatchable();
        assertNotNull(cmd);
        controller.recordDispatched(cmd.orderId(), 16);
        controller.removeSupply(PROVIDER);

        // Tick 100 times — reconciliation should skip this job (order already dispatched)
        for (int i = 0; i < 100; i++) {
            coordinator.tick(controller);
        }

        // Job should NOT have been invalidated (items are in transit)
        assertEquals(0, job.invalidatedAmount(),
                "Job should not be invalidated when order is already dispatched");
    }
}
