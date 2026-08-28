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
 * Wires {@link SteamEngineGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class SteamEngineGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "power/steam_engine_delivers_rf_from_pressure_then_sags",
            1020,
            SteamEngineGameTestBody::testSteamEngineDeliversRfFromPressureThenSags),
        new GameTestCase(
            "power/steam_engine_delivers_through_cable", 920, SteamEngineGameTestBody::testSteamEngineDeliversThroughCable),
        new GameTestCase(
            "power/steam_engine_holds_pressure_with_no_consumer",
            820,
            SteamEngineGameTestBody::testSteamEngineHoldsPressureWithNoConsumer),
        new GameTestCase("power/water_tank_accepts_only_water", 100, SteamEngineGameTestBody::testWaterTankAcceptsOnlyWater));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private SteamEngineGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/steam_engine", TESTS, FUNCTIONS);
    }
}
