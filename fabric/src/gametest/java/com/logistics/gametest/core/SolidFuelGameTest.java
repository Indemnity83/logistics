package com.logistics.gametest.core;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the solid-fuel GameTests. Test logic lives in
 * {@link SolidFuelGameTestBody} (shared with NeoForge — see {@code common/src/gametest}).
 */
public class SolidFuelGameTest {

    @GameTest(maxTicks = 10)
    public void testPeatBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testPeatBurns(context);
    }

    @GameTest(maxTicks = 10)
    public void testBitumenBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testBitumenBurns(context);
    }

    @GameTest(maxTicks = 10)
    public void testTarBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testTarBurns(context);
    }
}
