package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the engine redstone-mode GameTests. Test logic lives in
 * {@link EngineRedstoneGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class EngineRedstoneGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testStirlingEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testStirlingEngineIgnoredAndLow(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testFuelEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testFuelEngineIgnoredAndLow(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testMagmaticEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testMagmaticEngineIgnoredAndLow(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testSteamEngineIgnoredAndLow(GameTestHelper context) {
        EngineRedstoneGameTestBody.testSteamEngineIgnoredAndLow(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testReactionEngineIgnitesWhenIgnored(GameTestHelper context) {
        EngineRedstoneGameTestBody.testReactionEngineIgnitesWhenIgnored(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testReactionEngineLowHoldsIgnitionWhileSignalled(GameTestHelper context) {
        EngineRedstoneGameTestBody.testReactionEngineLowHoldsIgnitionWhileSignalled(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testEngineModePersists(GameTestHelper context) {
        EngineRedstoneGameTestBody.testEngineModePersists(context);
    }
}
