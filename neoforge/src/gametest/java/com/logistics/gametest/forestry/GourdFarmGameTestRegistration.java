package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link GourdFarmGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class GourdFarmGameTestRegistration {

    private GourdFarmGameTestRegistration() {}

    @GameTest(template = "empty", batch = "gourdfarm", timeoutTicks = 1200)
    public static void testHarvestsFruitNeverStemsAndHopperPullsHarvest(GameTestHelper context) {
        GourdFarmGameTestBody.testHarvestsFruitNeverStemsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "empty", batch = "gourdfarm", timeoutTicks = 1200)
    public static void testFullFarmSpillsAtTheStem(GameTestHelper context) {
        GourdFarmGameTestBody.testFullFarmSpillsAtTheStem(context);
    }

    @GameTest(template = "empty", batch = "gourdfarm", timeoutTicks = 1200)
    public static void testBrokenStemIsReTilledAndReplanted(GameTestHelper context) {
        GourdFarmGameTestBody.testBrokenStemIsReTilledAndReplanted(context);
    }
}
