package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Arboretum GameTests. Test logic lives in
 * {@link ArboretumGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class ArboretumGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testPlantsSaplingsOnLattice(GameTestHelper context) {
        ArboretumGameTestBody.testPlantsSaplingsOnLattice(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 3000)
    public void testFellsGrownTreeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsGrownTreeAndReplants(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 300)
    public void testNoPowerPlantsNothing(GameTestHelper context) {
        ArboretumGameTestBody.testNoPowerPlantsNothing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 300)
    public void testRedstonePausesFarm(GameTestHelper context) {
        ArboretumGameTestBody.testRedstonePausesFarm(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testClearsBlocksFromRing(GameTestHelper context) {
        ArboretumGameTestBody.testClearsBlocksFromRing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 2000)
    public void testHumusReplacesDirtAcrossTheRing(GameTestHelper context) {
        ArboretumGameTestBody.testHumusReplacesDirtAcrossTheRing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        ArboretumGameTestBody.testEveryGermlingGrowsOnArboretumSoils(context);
    }
}
