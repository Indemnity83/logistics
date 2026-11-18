package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CropFarmGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CropFarmGameTestRegistration {

    private CropFarmGameTestRegistration() {}

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 1200)
    public static void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 1200)
    public static void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        CropFarmGameTestBody.testFullFarmSpillsAtThePlot(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 1200)
    public static void testBreakingTheFarmForgetsItsPlots(GameTestHelper context) {
        CropFarmGameTestBody.testBreakingTheFarmForgetsItsPlots(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 400)
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 400)
    public static void testRedstoneSignalPausesFarm(GameTestHelper context) {
        CropFarmGameTestBody.testRedstoneSignalPausesFarm(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 1200)
    public static void testLeverOnEngineDoesNotPauseFarm(GameTestHelper context) {
        CropFarmGameTestBody.testLeverOnEngineDoesNotPauseFarm(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 100)
    public static void testTierSurvivesBreakAndReplace(GameTestHelper context) {
        CropFarmGameTestBody.testTierSurvivesBreakAndReplace(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 600)
    public static void testRaisingTierGrowsEnergyBuffer(GameTestHelper context) {
        CropFarmGameTestBody.testRaisingTierGrowsEnergyBuffer(context);
    }
}
