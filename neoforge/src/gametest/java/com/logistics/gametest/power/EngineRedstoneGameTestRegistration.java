package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link EngineRedstoneGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class EngineRedstoneGameTestRegistration {

    private EngineRedstoneGameTestRegistration() {}

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testStirlingEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testStirlingEngineIgnoredAndLow(context);
    }

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testFuelEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testFuelEngineIgnoredAndLow(context);
    }

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testMagmaticEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testMagmaticEngineIgnoredAndLow(context);
    }

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testSteamEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testSteamEngineIgnoredAndLow(context);
    }

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testReactionEngineIgnitesWhenIgnored(GameTestHelper context) {
        EngineRedstoneGameTestBody.testReactionEngineIgnitesWhenIgnored(context);
    }

    @GameTest(template = "empty", batch = "engineredstone", timeoutTicks = 100)
    public static void testReactionEngineLowHoldsIgnitionWhileSignalled(GameTestHelper context) {
        EngineRedstoneGameTestBody.testReactionEngineLowHoldsIgnitionWhileSignalled(context);
    }

    @GameTest(template = "empty", batch = "engineredstone")
    public static void testEngineModePersists(GameTestHelper context) {
        EngineRedstoneGameTestBody.testEngineModePersists(context);
    }
}
