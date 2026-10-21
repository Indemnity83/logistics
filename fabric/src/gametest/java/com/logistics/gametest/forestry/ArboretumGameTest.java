package com.logistics.gametest.forestry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Arboretum GameTests. Test logic lives in
 * {@link ArboretumGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class ArboretumGameTest {

    @GameTest(maxTicks = 2400)
    public void testFellsHumusTreeWholeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsHumusTreeWholeAndReplants(context);
    }

    @GameTest(maxTicks = 600)
    public void testLeavesTreeOnGrassStanding(GameTestHelper context) {
        ArboretumGameTestBody.testLeavesTreeOnGrassStanding(context);
    }

    @GameTest(maxTicks = 400)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        ArboretumGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(maxTicks = 400)
    public void testRedstoneSignalPausesFarm(GameTestHelper context) {
        ArboretumGameTestBody.testRedstoneSignalPausesFarm(context);
    }
}
