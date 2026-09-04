package com.logistics.pipe.network;

import com.logistics.core.lib.network.CrafterBufferState;
import com.logistics.core.lib.network.CrafterSnapshot;
import com.logistics.core.lib.network.FulfillmentMode;
import com.logistics.core.lib.network.PlanningView;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.test.MinecraftTestEnvironment;
import com.logistics.test.TestItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RequestPlanner}.
 *
 * <p>Uses {@link NetworkController} as the {@link PlanningView} implementation
 * because it is pure Java and already tested in isolation.
 */
class RequestPlannerTest extends MinecraftTestEnvironment {

    private RequestPlanner planner;
    private NetworkController controller;

    private static final BlockPos PROVIDER1 = new BlockPos(0, 0, 0);
    private static final BlockPos PROVIDER2 = new BlockPos(10, 0, 0);
    private static final BlockPos CRAFTER = new BlockPos(20, 0, 0);
    private static final BlockPos CRAFTER2 = new BlockPos(21, 0, 0);

    @BeforeEach
    void setUp() {
        planner = new RequestPlanner();
        controller = new NetworkController();
    }

    private IItemKey diamond() {
        return new TestItemKey(Items.DIAMOND);
    }

    private IItemKey emerald() {
        return new TestItemKey(Items.EMERALD);
    }

    private IItemKey oakPlanks() {
        return new TestItemKey(Items.OAK_PLANKS);
    }

    private IItemKey oakLog() {
        return new TestItemKey(Items.OAK_LOG);
    }

    // ===== Stock-first planning =====

    @Test
    void testPlanFromStock_allFromStock() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);

        FulfillmentPlan plan = planner.plan(diamond(), 16, FulfillmentMode.PARTIAL, controller);

        assertEquals(16, plan.plannedAmount());
        assertTrue(plan.full());
        assertEquals(1, plan.roots().size());
        assertInstanceOf(PlanNode.ExtractNode.class, plan.roots().getFirst());
        assertEquals(PROVIDER1, ((PlanNode.ExtractNode) plan.roots().getFirst()).provider());
    }

    @Test
    void testPlanFromCrafter_whenNoStock() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());

        FulfillmentPlan plan = planner.plan(diamond(), 8, FulfillmentMode.PARTIAL, controller);

        assertEquals(8, plan.plannedAmount());
        assertEquals(1, plan.roots().size());
        assertInstanceOf(PlanNode.CraftNode.class, plan.roots().getFirst());
        assertEquals(CRAFTER, ((PlanNode.CraftNode) plan.roots().getFirst()).crafter());
    }

    @Test
    void testStockBeforeCrafter_prefersRealStock() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 32L), 1);
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());

        FulfillmentPlan plan = planner.plan(diamond(), 32, FulfillmentMode.PARTIAL, controller);

        assertEquals(32, plan.plannedAmount());
        assertEquals(1, plan.roots().size());
        // Should only have an ExtractNode (all from stock)
        assertInstanceOf(PlanNode.ExtractNode.class, plan.roots().getFirst());
    }

    @Test
    void testPartialStockThenCrafter_mixedNodes() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 10L), 1);
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());

        FulfillmentPlan plan = planner.plan(diamond(), 20, FulfillmentMode.PARTIAL, controller);

        assertEquals(20, plan.plannedAmount());
        assertEquals(2, plan.roots().size());
        assertInstanceOf(PlanNode.ExtractNode.class, plan.roots().get(0));
        assertInstanceOf(PlanNode.CraftNode.class, plan.roots().get(1));
        assertEquals(10, ((PlanNode.ExtractNode) plan.roots().get(0)).amount());
    }

    // ===== FulfillmentMode =====

    @Test
    void testFullMode_returnsEmptyWhenCannotFullySatisfy() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 5L), 1);

        FulfillmentPlan plan = planner.plan(diamond(), 10, FulfillmentMode.FULL, controller);

        assertTrue(plan.isEmpty(), "FULL mode should return empty plan when supply < demand");
    }

    @Test
    void testFullMode_returnsValidPlanWhenEnoughStock() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 64L), 1);

        FulfillmentPlan plan = planner.plan(diamond(), 10, FulfillmentMode.FULL, controller);

        assertFalse(plan.isEmpty());
        assertEquals(10, plan.plannedAmount());
    }

    @Test
    void testPartialMode_returnsPartialPlanWhenSupplyInsufficient() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 5L), 1);

        FulfillmentPlan plan = planner.plan(diamond(), 10, FulfillmentMode.PARTIAL, controller);

        assertFalse(plan.isEmpty());
        assertEquals(5, plan.plannedAmount());
        assertFalse(plan.full());
    }

    @Test
    void testNoSupply_returnsEmptyPlan() {
        FulfillmentPlan plan = planner.plan(diamond(), 10, FulfillmentMode.PARTIAL, controller);

        assertTrue(plan.isEmpty());
        assertEquals(0, plan.plannedAmount());
    }

    // ===== Crafter buffer capping =====

    @Test
    void testCrafterWithSnapshot_capsToBufferCapacity() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());
        // Buffer can only accept 3 batches; recipe produces 4 per batch
        CrafterBufferState buffer = new CrafterBufferState(3, 3);
        CrafterSnapshot snapshot = new CrafterSnapshot(CRAFTER, diamond(), 4L, List.of(), buffer);
        controller.registerCrafterSnapshot(CRAFTER, snapshot);

        FulfillmentPlan plan = planner.plan(diamond(), 64, FulfillmentMode.PARTIAL, controller);

        // 3 batches × 4 per batch = 12 max; even though 64 requested
        assertEquals(12, plan.plannedAmount());
        PlanNode.CraftNode craftNode = (PlanNode.CraftNode) plan.roots().getFirst();
        assertEquals(3, craftNode.batches());
    }

    @Test
    void testCrafter_planNeverExceedsWhatWasRequested() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());
        CrafterBufferState buffer = new CrafterBufferState(8, 8);
        // A 4-per-batch recipe asked for 3: one batch must be crafted, but only 3 were requested.
        CrafterSnapshot snapshot = new CrafterSnapshot(CRAFTER, diamond(), 4L, List.of(), buffer);
        controller.registerCrafterSnapshot(CRAFTER, snapshot);

        FulfillmentPlan plan = planner.plan(diamond(), 3, FulfillmentMode.PARTIAL, controller);

        assertEquals(3, plan.plannedAmount(),
                "Rounding a batch up is fine; ordering the surplus onto the network is not");
        PlanNode.CraftNode craftNode = (PlanNode.CraftNode) plan.roots().getFirst();
        assertEquals(1, craftNode.batches(), "The batch itself still rounds up");
    }

    @Test
    void testCrafterWithFullBuffer_returnsNoCraftNode() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());
        CrafterSnapshot snapshot = new CrafterSnapshot(CRAFTER, diamond(), 4L, List.of(), CrafterBufferState.FULL);
        controller.registerCrafterSnapshot(CRAFTER, snapshot);

        FulfillmentPlan plan = planner.plan(diamond(), 16, FulfillmentMode.PARTIAL, controller);

        // Full buffer means 0 batch capacity → nothing planned from this crafter
        assertEquals(0, plan.plannedAmount());
        assertTrue(plan.isEmpty());
    }

    @Test
    void testCrafterWithoutSnapshot_treatedAsUnlimitedCapacity() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        controller.registerProviderCheck(CRAFTER, (amount, checker) -> List.of());
        // No snapshot registered

        FulfillmentPlan plan = planner.plan(diamond(), 64, FulfillmentMode.PARTIAL, controller);

        assertEquals(64, plan.plannedAmount());
    }

    // ===== Crafter without registered check =====

    @Test
    void testCrafterWithoutCheck_notUsed() {
        controller.registerSupply(CRAFTER, Map.of(diamond(), 0L), 5);
        // No registerProviderCheck call

        FulfillmentPlan plan = planner.plan(diamond(), 8, FulfillmentMode.PARTIAL, controller);

        assertEquals(0, plan.plannedAmount());
        assertTrue(plan.isEmpty());
    }

    // ===== Shared claimed scratch-pad =====

    @Test
    void testMultipleItems_claimedStockNotDoubleUsed() {
        // Two separate plans within same planning context (via PlanningContext inside planner)
        // Each plan call creates a fresh context, so this tests the per-call isolation
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 5L), 1);

        FulfillmentPlan plan1 = planner.plan(diamond(), 5, FulfillmentMode.PARTIAL, controller);
        FulfillmentPlan plan2 = planner.plan(diamond(), 5, FulfillmentMode.PARTIAL, controller);

        // Each call sees full supply (PlanningContext is per-call)
        assertEquals(5, plan1.plannedAmount());
        assertEquals(5, plan2.plannedAmount());
    }

    // ===== FulfillmentPlan structure =====

    @Test
    void testEmptyPlan_isEmpty() {
        FulfillmentPlan empty = FulfillmentPlan.empty();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.plannedAmount());
        assertTrue(empty.roots().isEmpty());
    }

    @Test
    void testExtractNode_hasCorrectFields() {
        controller.registerSupply(PROVIDER1, Map.of(diamond(), 20L), 1);

        FulfillmentPlan plan = planner.plan(diamond(), 10, FulfillmentMode.PARTIAL, controller);

        PlanNode.ExtractNode node = (PlanNode.ExtractNode) plan.roots().getFirst();
        assertEquals(PROVIDER1, node.provider());
        assertEquals(diamond(), node.item());
        assertEquals(10, node.amount());
    }
}
