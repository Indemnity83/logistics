package com.logistics.gametest.pipe;

import com.logistics.gametest.GameTestCase;
import com.logistics.gametest.GameTestRegistrationSupport;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Wires {@link ModuleGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class ModuleGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("pipe/filter_pipe_placement", 100, ModuleGameTestBody::testFilterPipePlacement),
        new GameTestCase("pipe/extractor_pipe_placement", 100, ModuleGameTestBody::testExtractorPipePlacement),
        new GameTestCase("pipe/merger_pipe_placement", 100, ModuleGameTestBody::testMergerPipePlacement),
        new GameTestCase("pipe/insertion_pipe_placement", 100, ModuleGameTestBody::testInsertionPipePlacement),
        new GameTestCase("pipe/void_pipe_placement", 100, ModuleGameTestBody::testVoidPipePlacement),
        new GameTestCase("pipe/passthrough_pipe_placement", 100, ModuleGameTestBody::testPassthroughPipePlacement),
        new GameTestCase(
            "pipe/gold_transport_pipe_placement", 100, ModuleGameTestBody::testGoldTransportPipePlacement),
        new GameTestCase(
            "pipe/filter_module_routes_matching_items", 100, ModuleGameTestBody::testFilterModuleRoutesMatchingItems),
        new GameTestCase(
            "pipe/filter_module_passes_through_non_matching",
            100,
            ModuleGameTestBody::testFilterModulePassesThroughNonMatching),
        new GameTestCase(
            "pipe/merger_module_routes_to_output", 100, ModuleGameTestBody::testMergerModuleRoutesToOutput),
        new GameTestCase(
            "pipe/filter_module_multiple_side_filters",
            100,
            ModuleGameTestBody::testFilterModuleMultipleSideFilters),
        new GameTestCase(
            "pipe/insertion_module_delivers_to_adjacent_chest",
            60,
            ModuleGameTestBody::testInsertionModuleDeliversToAdjacentChest),
        new GameTestCase(
            "pipe/sink_module_filter_match_routes_to_inventory",
            80,
            ModuleGameTestBody::testSinkModuleFilterMatchRoutesToInventory),
        new GameTestCase(
            "pipe/sink_module_default_route_accepts_items",
            80,
            ModuleGameTestBody::testSinkModuleDefaultRouteAcceptsItems),
        new GameTestCase(
            "pipe/chassis_drops_modules_on_non_player_break",
            100,
            ModuleGameTestBody::testChassisDropsModulesOnNonPlayerBreak));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private ModuleGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "pipe/module", TESTS, FUNCTIONS);
    }
}
