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
 * Wires {@link AlloySmelterGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class AlloySmelterGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("automation/alloy_smelter_placement", 100, AlloySmelterGameTestBody::testPlacement),
        new GameTestCase(
            "automation/alloy_smelter_inputs_are_order_independent", 60, AlloySmelterGameTestBody::testInputsAreOrderIndependent),
        new GameTestCase(
            "automation/alloy_smelter_smelts_iron_ore_with_sand_flux", 240, AlloySmelterGameTestBody::testSmeltsIronOreWithSandFlux),
        new GameTestCase(
            "automation/alloy_smelter_alloys_copper_and_tin_into_bronze", 240, AlloySmelterGameTestBody::testAlloysCopperAndTinIntoBronze),
        new GameTestCase(
            "automation/alloy_smelter_smelts_iron_ore_via_real_engine_and_hoppers",
            280,
            AlloySmelterGameTestBody::testSmeltsIronOreViaRealEngineAndHoppers));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private AlloySmelterGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "automation/alloy_smelter", TESTS, FUNCTIONS);
    }
}
