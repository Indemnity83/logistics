package com.logistics.gametest.power;

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
 * Wires {@link EngineGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class EngineGameTestRegistration {

    // NeoForge's GameTestInstance ticks the environment/structure differently than Fabric's
    // @GameTest shim before handing control to the test body, so timed tests carry ~20 ticks more
    // headroom here than their Fabric @GameTest(maxTicks=...) counterpart for the same callbacks
    // to land within budget.
    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("power/redstone_engine_placement", 100, EngineGameTestBody::testRedstoneEnginePlacement),
        new GameTestCase("power/stirling_engine_placement", 100, EngineGameTestBody::testStirlingEnginePlacement),
        new GameTestCase("power/creative_engine_placement", 100, EngineGameTestBody::testCreativeEnginePlacement),
        new GameTestCase(
            "power/redstone_engine_cannot_overheat", 100, EngineGameTestBody::testRedstoneEngineCannotOverheat),
        new GameTestCase(
            "power/stirling_engine_inventory_not_accessible_from_front",
            100,
            EngineGameTestBody::testStirlingEngineInventoryNotAccessibleFromFront),
        new GameTestCase(
            "power/stirling_engine_inventory_accessible_from_other_sides",
            100,
            EngineGameTestBody::testStirlingEngineInventoryAccessibleFromOtherSides),
        new GameTestCase("power/stirling_engine_accepts_fuel", 100, EngineGameTestBody::testStirlingEngineAcceptsFuel),
        new GameTestCase(
            "power/stirling_engine_rejects_non_fuel", 100, EngineGameTestBody::testStirlingEngineRejectsNonFuel),
        new GameTestCase(
            "power/creative_engine_cannot_overheat", 100, EngineGameTestBody::testCreativeEngineCannotOverheat),
        new GameTestCase("power/creative_engine_output_levels", 100, EngineGameTestBody::testCreativeEngineOutputLevels),
        new GameTestCase("power/creative_sink_unlimited_drain", 100, EngineGameTestBody::testCreativeSinkUnlimitedDrain),
        new GameTestCase(
            "power/redstone_engine_produces_energy_when_powered",
            50,
            EngineGameTestBody::testRedstoneEngineProducesEnergyWhenPowered),
        new GameTestCase(
            "power/creative_engine_accumulates_energy_when_powered",
            100,
            EngineGameTestBody::testCreativeEngineAccumulatesEnergyWhenPowered),
        new GameTestCase(
            "power/stirling_engine_produces_energy_from_fuel",
            140,
            EngineGameTestBody::testStirlingEngineProducesEnergyFromFuel),
        new GameTestCase(
            "power/redstone_engine_produces_no_energy_when_unpowered",
            50,
            EngineGameTestBody::testRedstoneEngineProducesNoEnergyWhenUnpowered));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private EngineGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/engine", TESTS, FUNCTIONS);
    }
}
