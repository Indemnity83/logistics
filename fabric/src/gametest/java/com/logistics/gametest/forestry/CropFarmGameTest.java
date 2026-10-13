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
    public void testTillsAndPlantsWheat(GameTestHelper context) {
        CropFarmGameTestBody.testTillsAndPlantsWheat(context);
    }

    @GameTest(maxTicks = 1200)
    public void testIrrigatesFarmlandFromItsTank(GameTestHelper context) {
        CropFarmGameTestBody.testIrrigatesFarmlandFromItsTank(context);
    }

    @GameTest(maxTicks = 3000)
    public void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsFromReturnedSeeds(context);
    }

    @GameTest(maxTicks = 1000)
    public void testPlantsButReportsNoWater(GameTestHelper context) {
        CropFarmGameTestBody.testPlantsButReportsNoWater(context);
    }

    @GameTest(maxTicks = 300)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(maxTicks = 1200)
    public void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        CropFarmGameTestBody.testLeverOnEngineDoesNotPauseFarm(context);
    }

    @GameTest(maxTicks = 1200)
    public void testClearsTorchFromPlot(GameTestHelper context) {
        CropFarmGameTestBody.testClearsTorchFromPlot(context);
    }

    @GameTest(maxTicks = 1200)
    public void testClearsBlockAbovePlot(GameTestHelper context) {
        CropFarmGameTestBody.testClearsBlockAbovePlot(context);
    }

    @GameTest(maxTicks = 1200)
    public void testNeverBreaksChestInRing(GameTestHelper context) {
        CropFarmGameTestBody.testNeverBreaksChestInRing(context);
    }

    @GameTest(maxTicks = 1200)
    public void testLeavesInnerSquareAlone(GameTestHelper context) {
        CropFarmGameTestBody.testLeavesInnerSquareAlone(context);
    }

    @GameTest(maxTicks = 1200)
    public void testReplacesGrassWithDirt(GameTestHelper context) {
        CropFarmGameTestBody.testReplacesGrassWithDirt(context);
    }
}
