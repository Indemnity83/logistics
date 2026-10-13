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

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 800)
    public void testTillsAndPlantsWheat(GameTestHelper context) {
        CropFarmGameTestBody.testTillsAndPlantsWheat(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 800)
    public void testIrrigatesFarmlandFromItsTank(GameTestHelper context) {
        CropFarmGameTestBody.testIrrigatesFarmlandFromItsTank(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 2400)
    public void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsFromReturnedSeeds(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 600)
    public void testPlantsButReportsNoWater(GameTestHelper context) {
        CropFarmGameTestBody.testPlantsButReportsNoWater(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 300)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 800)
    public void testLeavesTorchOnPlot(GameTestHelper context) {
        CropFarmGameTestBody.testLeavesTorchOnPlot(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 800)
    public void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        CropFarmGameTestBody.testLeverOnEngineDoesNotPauseFarm(context);
    }
}
