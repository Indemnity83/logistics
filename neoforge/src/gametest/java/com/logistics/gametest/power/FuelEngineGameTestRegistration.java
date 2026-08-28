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
 * Wires {@link FuelEngineGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class FuelEngineGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase("power/fuel_engine_generates_from_fuel", 60, FuelEngineGameTestBody::testFuelEngineGeneratesFromFuel),
        new GameTestCase(
            "power/fuel_engine_overheats_without_coolant_then_resets",
            160,
            FuelEngineGameTestBody::testFuelEngineOverheatsWithoutCoolantThenResets),
        new GameTestCase("power/fluid_insert_routes_by_type", 100, FuelEngineGameTestBody::testFluidInsertRoutesByType));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private FuelEngineGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/fuel_engine", TESTS, FUNCTIONS);
    }
}
