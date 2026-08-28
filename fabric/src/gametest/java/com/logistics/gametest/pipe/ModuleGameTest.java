package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the pipe module GameTests. Test logic lives in
 * {@link ModuleGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these methods
 * only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery requires.
 */
public class ModuleGameTest {

    /**
    * Test that filter pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFilterPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testFilterPipePlacement(context);
    }

    /**
    * Test that extractor pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testExtractorPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testExtractorPipePlacement(context);
    }

    /**
    * Test that merger pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMergerPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testMergerPipePlacement(context);
    }

    /**
    * Test that insertion pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testInsertionPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testInsertionPipePlacement(context);
    }

    /**
    * Test that void pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testVoidPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testVoidPipePlacement(context);
    }

    /**
    * Test that passthrough pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testPassthroughPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testPassthroughPipePlacement(context);
    }

    /**
    * Test that gold transport pipes can be placed and have block entities.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testGoldTransportPipePlacement(GameTestHelper context) {
        ModuleGameTestBody.testGoldTransportPipePlacement(context);
    }

    /**
    * Test that filter module routes items based on configured filters.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFilterModuleRoutesMatchingItems(GameTestHelper context) {
        ModuleGameTestBody.testFilterModuleRoutesMatchingItems(context);
    }

    /**
    * Test that filter module passes through non-matching items.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFilterModulePassesThroughNonMatching(GameTestHelper context) {
        ModuleGameTestBody.testFilterModulePassesThroughNonMatching(context);
    }

    /**
    * Test that merger module routes items to configured output direction.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMergerModuleRoutesToOutput(GameTestHelper context) {
        ModuleGameTestBody.testMergerModuleRoutesToOutput(context);
    }

    /**
    * Test that filter module handles multiple filters on different sides.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testFilterModuleMultipleSideFilters(GameTestHelper context) {
        ModuleGameTestBody.testFilterModuleMultipleSideFilters(context);
    }

    /**
    * Verifies that InsertionModule routes an item into an adjacent chest when space is available.
    *
    * <p>InsertionModule.route() uses ItemStorage.SIDED.find() to probe real inventory capacity —
    * this path requires a live world and cannot be exercised in unit tests.
    *
    * <p>Layout (y=1): [chest at (0,1,0)] ← [insertion_pipe at (1,1,0)]
    * Diamond is injected from EAST (travels WEST toward the chest).
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testInsertionModuleDeliversToAdjacentChest(GameTestHelper context) {
        ModuleGameTestBody.testInsertionModuleDeliversToAdjacentChest(context);
    }

    /**
    * Verifies that SinkModule routes a filter-matched item into the adjacent inventory.
    *
    * <p>SinkModule.matchesFilter() calls BuiltInRegistries.ITEM.get() and then route() uses
    * the live connection cache (set by onConnectionsChanged) — both require a running game.
    *
    * <p>Layout (y=1): [chest at (0,1,0)] ← [basic_logistics_pipe at (1,1,0)]
    * Diamond filter on SinkModule; diamond injected from EAST routes WEST into the chest.
    *
    * <p>BASIC_LOGISTICS_PIPE includes NetworkRouterModule, which drops items when no sink is
    * registered in the network. SinkModule.onTick registers after SYNC_INTERVAL = 20 ticks,
    * so we inject the diamond at tick 22 to ensure the sink is already registered.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testSinkModuleFilterMatchRoutesToInventory(GameTestHelper context) {
        ModuleGameTestBody.testSinkModuleFilterMatchRoutesToInventory(context);
    }

    /**
    * Verifies that SinkModule with default route accepts any item that has no network destination.
    *
    * <p>With only the chest at WEST connected, validDirections = [WEST]. Since the sink direction
    * is the only option, hasOtherOptions = false and the default route fires.
    *
    * <p>Layout (y=1): [chest at (0,1,0)] ← [basic_logistics_pipe at (1,1,0)]
    * Default route enabled; dirt (no destination) injected from EAST routes WEST to chest.
    *
    * <p>BASIC_LOGISTICS_PIPE includes NetworkRouterModule, which drops items when no sink is
    * registered in the network. SinkModule.onTick registers the generic sink interest after
    * SYNC_INTERVAL = 20 ticks, so we inject the dirt at tick 22.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testSinkModuleDefaultRouteAcceptsItems(GameTestHelper context) {
        ModuleGameTestBody.testSinkModuleDefaultRouteAcceptsItems(context);
    }

    /**
    * A chassis pipe destroyed by a non-player removal (explosion, /setblock) must still drop its
    * installed modules with configuration intact. The drop runs in
    * {@code PipeBlockEntity.preRemoveSideEffects()}, which fires on every removal path -- not only
    * the player-break path -- so modules are no longer voided by explosions.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testChassisDropsModulesOnNonPlayerBreak(GameTestHelper context) {
        ModuleGameTestBody.testChassisDropsModulesOnNonPlayerBreak(context);
    }
}
