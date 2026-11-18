package com.logistics.gametest.forestry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Crop Farm GameTests. Test logic lives in
 * {@link CropFarmGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class CropFarmGameTest {

    @GameTest(maxTicks = 1200)
    public void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(maxTicks = 1200)
    public void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        CropFarmGameTestBody.testFullFarmSpillsAtThePlot(context);
    }

    @GameTest(maxTicks = 1200)
    public void testBreakingTheFarmForgetsItsPlots(GameTestHelper context) {
        CropFarmGameTestBody.testBreakingTheFarmForgetsItsPlots(context);
    }

    @GameTest(maxTicks = 400)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(maxTicks = 400)
    public void testRedstoneSignalPausesFarm(GameTestHelper context) {
        CropFarmGameTestBody.testRedstoneSignalPausesFarm(context);
    }

    @GameTest(maxTicks = 1200)
    public void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        CropFarmGameTestBody.testLeverOnEngineDoesNotPauseFarm(context);
    }

    @GameTest(maxTicks = 100)
    public void testTierSurvivesBreakAndReplace(GameTestHelper context) {
        CropFarmGameTestBody.testTierSurvivesBreakAndReplace(context);
    }

    @GameTest(maxTicks = 600)
    public void testRaisingTierGrowsEnergyBuffer(GameTestHelper context) {
        CropFarmGameTestBody.testRaisingTierGrowsEnergyBuffer(context);
    }
}
