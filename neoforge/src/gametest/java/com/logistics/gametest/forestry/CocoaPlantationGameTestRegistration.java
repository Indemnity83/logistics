package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CocoaPlantationGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CocoaPlantationGameTestRegistration {

    private CocoaPlantationGameTestRegistration() {}

    @GameTest(template = "empty", batch = "cocoaplantation", timeoutTicks = 1200)
    public static void testPicksAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        CocoaPlantationGameTestBody.testPicksAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "empty", batch = "cocoaplantation", timeoutTicks = 1200)
    public static void testHopperFedBeansReplantEmptyPlots(GameTestHelper context) {
        CocoaPlantationGameTestBody.testHopperFedBeansReplantEmptyPlots(context);
    }

    @GameTest(template = "empty", batch = "cocoaplantation", timeoutTicks = 1200)
    public static void testPicksAndRefillsPodOutsideReachOnLogInside(GameTestHelper context) {
        CocoaPlantationGameTestBody.testPicksAndRefillsPodOutsideReachOnLogInside(context);
    }

    @GameTest(template = "empty", batch = "cocoaplantation", timeoutTicks = 400)
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        CocoaPlantationGameTestBody.testDoesNothingWithoutPower(context);
    }
}
