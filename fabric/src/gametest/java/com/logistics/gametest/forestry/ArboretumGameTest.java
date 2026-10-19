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

    @GameTest(maxTicks = 1200)
    public void testPlantsSaplingsOnLattice(GameTestHelper context) {
        ArboretumGameTestBody.testPlantsSaplingsOnLattice(context);
    }

    @GameTest(maxTicks = 3000)
    public void testFellsGrownTreeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsGrownTreeAndReplants(context);
    }

    @GameTest(maxTicks = 300)
    public void testNoPowerPlantsNothing(GameTestHelper context) {
        ArboretumGameTestBody.testNoPowerPlantsNothing(context);
    }

    @GameTest(maxTicks = 300)
    public void testRedstoneIgnoredByDefault(GameTestHelper context) {
        ArboretumGameTestBody.testRedstoneIgnoredByDefault(context);
    }

    @GameTest(maxTicks = 600)
    public void testSignalPausesFarmInLowMode(GameTestHelper context) {
        ArboretumGameTestBody.testSignalPausesFarmInLowMode(context);
    }

    @GameTest(maxTicks = 1200)
    public void testClearsBlocksFromRing(GameTestHelper context) {
        ArboretumGameTestBody.testClearsBlocksFromRing(context);
    }

    @GameTest(maxTicks = 2000)
    public void testHumusReplacesDirtAcrossTheRing(GameTestHelper context) {
        ArboretumGameTestBody.testHumusReplacesDirtAcrossTheRing(context);
    }

    @GameTest(maxTicks = 2000)
    public void testLaysHumusLevelWithFarmOnGrass(GameTestHelper context) {
        ArboretumGameTestBody.testLaysHumusLevelWithFarmOnGrass(context);
    }

    @GameTest(maxTicks = 2000)
    public void testFelledTreeLeavesSandThatIsRelaidWithHumus(GameTestHelper context) {
        ArboretumGameTestBody.testFelledTreeLeavesSandThatIsRelaidWithHumus(context);
    }

    @GameTest(maxTicks = 2000)
    public void testFelled2x2TreeLeavesSandUnderAllFourCells(GameTestHelper context) {
        ArboretumGameTestBody.testFelled2x2TreeLeavesSandUnderAllFourCells(context);
    }

    @GameTest(maxTicks = 1200)
    public void testWithoutHumusSandIsLeftUnplanted(GameTestHelper context) {
        ArboretumGameTestBody.testWithoutHumusSandIsLeftUnplanted(context);
    }

    @GameTest(maxTicks = 40)
    public void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        ArboretumGameTestBody.testEveryGermlingGrowsOnArboretumSoils(context);
    }
}
