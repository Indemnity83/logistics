package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the solid-fuel GameTests. Test logic lives in
 * {@link SolidFuelGameTestBody} (shared with NeoForge — see {@code common/src/gametest}).
 */
public class SolidFuelGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 10)
    public void testPeatBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testPeatBurns(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 10)
    public void testBitumenBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testBitumenBurns(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 10)
    public void testTarBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testTarBurns(context);
    }
}
