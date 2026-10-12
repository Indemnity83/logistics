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

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 800)
    public static void testTillsAndPlantsWheat(GameTestHelper context) {
        CropFarmGameTestBody.testTillsAndPlantsWheat(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 800)
    public static void testIrrigatesFarmlandFromItsTank(GameTestHelper context) {
        CropFarmGameTestBody.testIrrigatesFarmlandFromItsTank(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 2400)
    public static void testHarvestsAndReplantsFromReturnedSeeds(GameTestHelper context) {
        CropFarmGameTestBody.testHarvestsAndReplantsFromReturnedSeeds(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 620)
    public static void testPlantsButReportsNoWater(GameTestHelper context) {
        CropFarmGameTestBody.testPlantsButReportsNoWater(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 300)
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        CropFarmGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(template = "empty", batch = "cropfarm", timeoutTicks = 800)
    public static void testLeavesTorchOnPlot(GameTestHelper context) {
        CropFarmGameTestBody.testLeavesTorchOnPlot(context);
    }
}
