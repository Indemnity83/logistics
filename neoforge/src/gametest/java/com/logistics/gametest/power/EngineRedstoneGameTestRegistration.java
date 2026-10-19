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
 * Wires {@link EngineRedstoneGameTestBody}'s methods into MC's data-driven GameTest registries — see
 * {@link GameTestRegistrationSupport}.
 */
@EventBusSubscriber(modid = "logistics_gametest")
public final class EngineRedstoneGameTestRegistration {

    private static final List<GameTestCase> TESTS = List.of(
        new GameTestCase(
            "power/engine_redstone_stirling_ignored_and_low", 120, EngineRedstoneGameTestBody::testStirlingEngineIgnoredAndLow),
        new GameTestCase(
            "power/engine_redstone_fuel_ignored_and_low", 120, EngineRedstoneGameTestBody::testFuelEngineIgnoredAndLow),
        new GameTestCase(
            "power/engine_redstone_magmatic_ignored_and_low", 120, EngineRedstoneGameTestBody::testMagmaticEngineIgnoredAndLow),
        new GameTestCase(
            "power/engine_redstone_steam_ignored_and_low", 120, EngineRedstoneGameTestBody::testSteamEngineIgnoredAndLow),
        new GameTestCase(
            "power/engine_redstone_reaction_ignites_when_ignored",
            120,
            EngineRedstoneGameTestBody::testReactionEngineIgnitesWhenIgnored),
        new GameTestCase(
            "power/engine_redstone_reaction_low_holds_ignition_while_signalled",
            120,
            EngineRedstoneGameTestBody::testReactionEngineLowHoldsIgnitionWhileSignalled),
        new GameTestCase("power/engine_redstone_mode_persists", 100, EngineRedstoneGameTestBody::testEngineModePersists));

    private static final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> FUNCTIONS =
        GameTestRegistrationSupport.registerFunctions(TESTS);

    private EngineRedstoneGameTestRegistration() {}

    /** Forces this class's static initializer to run — see {@code LogisticsGameTestMod}. */
    public static void bootstrap() {}

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        GameTestRegistrationSupport.registerInstances(event, "power/engine_redstone", TESTS, FUNCTIONS);
    }
}
