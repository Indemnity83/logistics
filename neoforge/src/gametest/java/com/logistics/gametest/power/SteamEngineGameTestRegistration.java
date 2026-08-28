package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link SteamEngineGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class SteamEngineGameTestRegistration {

    private SteamEngineGameTestRegistration() {}

    /**
    * With a consumer on the output face, the engine first <em>heats</em> the boiler (~500+ ticks past the
    * boiling point) before steam builds pressure, which it pushes as RF <em>directly</em> (no RF buffer,
    * no energy capability) — the battery accumulates it. Then draining water sags the pressure.
    */
    @GameTest(template = "empty", batch = "steamengine", timeoutTicks = 1000)
    public static void testSteamEngineDeliversRfFromPressureThenSags(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineDeliversRfFromPressureThenSags(context);
    }

    /** RF pushed from pressure flows through a cable to a consumer, even with no engine capability. */
    @GameTest(template = "empty", batch = "steamengine", timeoutTicks = 900)
    public static void testSteamEngineDeliversThroughCable(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineDeliversThroughCable(context);
    }

    /** Once hot, the boiler builds pressure with no consumer but the turbine delivers nothing. */
    @GameTest(template = "empty", batch = "steamengine", timeoutTicks = 800)
    public static void testSteamEngineHoldsPressureWithNoConsumer(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineHoldsPressureWithNoConsumer(context);
    }

    /** The water-only fluid view accepts water into the boiler tank and rejects other fluids. */
    @GameTest(template = "empty", batch = "steamengine")
    public static void testWaterTankAcceptsOnlyWater(GameTestHelper context) {
        SteamEngineGameTestBody.testWaterTankAcceptsOnlyWater(context);
    }
}
