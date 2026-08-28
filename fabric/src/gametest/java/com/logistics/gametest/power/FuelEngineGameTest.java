package com.logistics.gametest.power;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fuel engine GameTests. Test logic lives in
 * {@link FuelEngineGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class FuelEngineGameTest {

    /** A powered engine with fuel + water generates RF and warms up. */
    @GameTest(maxTicks = 40)
    public void testFuelEngineGeneratesFromFuel(GameTestHelper context) {
        FuelEngineGameTestBody.testFuelEngineGeneratesFromFuel(context);
    }

    /** Without coolant the engine overheats; a wrench-style reset clears the shutdown and preserves tank fuel. */
    @GameTest(maxTicks = 140)
    public void testFuelEngineOverheatsWithoutCoolantThenResets(GameTestHelper context) {
        FuelEngineGameTestBody.testFuelEngineOverheatsWithoutCoolantThenResets(context);
    }

    /** The combined fluid view routes inserts by type: water to the coolant tank, fuel to the fuel tank. */
    @GameTest
    public void testFluidInsertRoutesByType(GameTestHelper context) {
        FuelEngineGameTestBody.testFluidInsertRoutesByType(context);
    }
}
