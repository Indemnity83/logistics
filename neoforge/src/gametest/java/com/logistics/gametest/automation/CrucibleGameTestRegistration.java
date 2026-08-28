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
 * Wires {@link CrucibleGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class CrucibleGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/crucible_placement", 100, CrucibleGameTestBody::testPlacement),
        new GameTestCase("automation/crucible_tank_is_output_only", 100, CrucibleGameTestBody::testTankIsOutputOnly),
        new GameTestCase("automation/crucible_melts_ice_into_water", 80, CrucibleGameTestBody::testMeltsIceIntoWater),
        new GameTestCase(
            "automation/crucible_melts_via_real_engine_and_hopper", 120, CrucibleGameTestBody::testMeltsViaRealEngineAndHopper));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private CrucibleGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/crucible", TESTS, FUNCTIONS);
    }
}
