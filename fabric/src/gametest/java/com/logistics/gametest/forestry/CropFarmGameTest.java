package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Crop Farm GameTests. Test logic lives in
 * {@link CropFarmGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class CropFarmGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testTillsAndPlantsWheat(GameTestHelper context) {
        CropFarmGameTestBody.testTillsAndPlantsWheat(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testTillsAndPlantsWithoutWater(GameTestHelper context) {
        CropFarmGameTestBody.testTillsAndPlantsWithoutWater(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 2400)
    public void testHarvestKeepsFarmland(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestKeepsFarmland(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testIrrigatesRingFarmlandFromTank(GameTestHelper context) {
        CropFarmGameTestBody.testIrrigatesRingFarmlandFromTank(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testFarmlandDriesWithoutWater(GameTestHelper context) {
        CropFarmGameTestBody.testFarmlandDriesWithoutWater(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testSurveyingFarmDoesNotIrrigate(GameTestHelper context) {
        CropFarmGameTestBody.testSurveyingFarmDoesNotIrrigate(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testNeverIrrigatesOutsideRing(GameTestHelper context) {
        CropFarmGameTestBody.testNeverIrrigatesOutsideRing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 3000)
    public void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsFromReturnedSeeds(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 300)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        CropFarmGameTestBody.testLeverOnEngineDoesNotPauseFarm(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testClearsTorchFromPlot(GameTestHelper context) {
        CropFarmGameTestBody.testClearsTorchFromPlot(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testClearsBlockAbovePlot(GameTestHelper context) {
        CropFarmGameTestBody.testClearsBlockAbovePlot(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testNeverBreaksChestInRing(GameTestHelper context) {
        CropFarmGameTestBody.testNeverBreaksChestInRing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testLeavesInnerSquareAlone(GameTestHelper context) {
        CropFarmGameTestBody.testLeavesInnerSquareAlone(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testTillsGrassInPlace(GameTestHelper context) {
        CropFarmGameTestBody.testTillsGrassInPlace(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 2400)
    public void testLaysSoilLevelWithFarmOnGrass(GameTestHelper context) {
        CropFarmGameTestBody.testLaysSoilLevelWithFarmOnGrass(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 100)
    public void testTierSurvivesBreakAndReplace(GameTestHelper context) {
        CropFarmGameTestBody.testTierSurvivesBreakAndReplace(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 600)
    public void testRaisingTierGrowsEnergyBuffer(GameTestHelper context) {
        CropFarmGameTestBody.testRaisingTierGrowsEnergyBuffer(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testSurveyChangesNothingUntilStart(GameTestHelper context) {
        CropFarmGameTestBody.testSurveyChangesNothingUntilStart(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 600)
    public void testUpgradeWorksNewLanesImmediately(GameTestHelper context) {
        CropFarmGameTestBody.testUpgradeWorksNewLanesImmediately(context);
    }
}
