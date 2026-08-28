package com.logistics.gametest.power;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the steam engine GameTests. Test logic lives in
 * {@link SteamEngineGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class SteamEngineGameTest {

    /**
     * With a consumer on the output face, the engine first <em>heats</em> the boiler (~500+ ticks past the
     * boiling point) before steam builds pressure, which it pushes as RF <em>directly</em> (no RF buffer,
     * no energy capability) — the battery accumulates it. Then draining water sags the pressure.
     */
    @GameTest(maxTicks = 1000)
    public void testSteamEngineDeliversRfFromPressureThenSags(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineDeliversRfFromPressureThenSags(context);
    }

    /** RF pushed from pressure flows through a cable to a consumer, even with no engine capability. */
    @GameTest(maxTicks = 900)
    public void testSteamEngineDeliversThroughCable(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineDeliversThroughCable(context);
    }

    /** Once hot, the boiler builds pressure with no consumer but the turbine delivers nothing. */
    @GameTest(maxTicks = 800)
    public void testSteamEngineHoldsPressureWithNoConsumer(GameTestHelper context) {
        SteamEngineGameTestBody.testSteamEngineHoldsPressureWithNoConsumer(context);
    }

    /** The water-only fluid view accepts water into the boiler tank and rejects other fluids. */
    @GameTest
    public void testWaterTankAcceptsOnlyWater(GameTestHelper context) {
        SteamEngineGameTestBody.testWaterTankAcceptsOnlyWater(context);
    }
}
