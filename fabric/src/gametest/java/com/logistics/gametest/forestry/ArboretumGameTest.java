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

    @GameTest(maxTicks = 600)
    public void testPlantsSaplingsOnLattice(GameTestHelper context) {
        ArboretumGameTestBody.testPlantsSaplingsOnLattice(context);
    }

    @GameTest(maxTicks = 2000)
    public void testFellsGrownTreeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsGrownTreeAndReplants(context);
    }

    @GameTest(maxTicks = 300)
    public void testNoPowerPlantsNothing(GameTestHelper context) {
        ArboretumGameTestBody.testNoPowerPlantsNothing(context);
    }

    @GameTest(maxTicks = 300)
    public void testRedstonePausesFarm(GameTestHelper context) {
        ArboretumGameTestBody.testRedstonePausesFarm(context);
    }

    @GameTest(maxTicks = 300)
    public void testLeavesStoneAndPlayerBuildsAlone(GameTestHelper context) {
        ArboretumGameTestBody.testLeavesStoneAndPlayerBuildsAlone(context);
    }

    @GameTest(maxTicks = 600)
    public void testHumusReplacesDirtUnderPlantingCorners(GameTestHelper context) {
        ArboretumGameTestBody.testHumusReplacesDirtUnderPlantingCorners(context);
    }

    @GameTest(maxTicks = 40)
    public void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        ArboretumGameTestBody.testEveryGermlingGrowsOnArboretumSoils(context);
    }
}
