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
 * Wires {@link SawmillGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class SawmillGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "automation/sawmill_placement_creates_block_entity", 100, SawmillGameTestBody::testPlacementCreatesBlockEntity),
        new GameTestCase("automation/sawmill_sided_access", 100, SawmillGameTestBody::testSidedAccess),
        new GameTestCase("automation/sawmill_capabilities_exposed", 100, SawmillGameTestBody::testCapabilitiesExposed),
        new GameTestCase(
            "automation/sawmill_saws_log_into_planks_and_byproduct", 220, SawmillGameTestBody::testSawsLogIntoPlanksAndByproduct),
        new GameTestCase(
            "automation/sawmill_accepts_kelp_and_seeds_as_input", 100, SawmillGameTestBody::testAcceptsKelpAndSeedsAsInput),
        new GameTestCase(
            "automation/sawmill_accepts_single_kelp_from_a_pipe_probe", 100, SawmillGameTestBody::testAcceptsSingleKelpFromAPipeProbe),
        new GameTestCase("automation/sawmill_pulps_kelp_into_biomass", 170, SawmillGameTestBody::testPulpsKelpIntoBiomass),
        new GameTestCase("automation/sawmill_pulps_seeds_into_biomass", 170, SawmillGameTestBody::testPulpsSeedsIntoBiomass),
        new GameTestCase(
            "automation/sawmill_saws_via_real_engine_and_hoppers", 260, SawmillGameTestBody::testSawsViaRealEngineAndHoppers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private SawmillGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/sawmill", TESTS, FUNCTIONS);
    }
}
