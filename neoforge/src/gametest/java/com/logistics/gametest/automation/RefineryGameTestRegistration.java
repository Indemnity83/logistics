package com.logistics.gametest.automation;

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
 * Wires {@link RefineryGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class RefineryGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/refinery_placement", 100, RefineryGameTestBody::testPlacement),
        new GameTestCase(
            "automation/refinery_insert_targets_input_extract_targets_output",
            100,
            RefineryGameTestBody::testInsertTargetsInputExtractTargetsOutput),
        new GameTestCase(
            "automation/refinery_distills_liquid_biomass_into_bio_fuel", 300, RefineryGameTestBody::testDistillsLiquidBiomassIntoBioFuel),
        new GameTestCase("automation/refinery_distills_via_real_engine", 340, RefineryGameTestBody::testDistillsViaRealEngine));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private RefineryGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/refinery", TESTS, FUNCTIONS);
    }
}
