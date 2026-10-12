package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link PlantCompatGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class PlantCompatGameTestRegistration {

    private PlantCompatGameTestRegistration() {}

    @GameTest(template = "empty", batch = "plantcompat", timeoutTicks = 40)
    public static void testFertilizeAdvancesWheat(GameTestHelper context) {
        PlantCompatGameTestBody.testFertilizeAdvancesWheat(context);
    }

    @GameTest(template = "empty", batch = "plantcompat", timeoutTicks = 40)
    public static void testFertilizeRejectsNonBonemealableBlock(GameTestHelper context) {
        PlantCompatGameTestBody.testFertilizeRejectsNonBonemealableBlock(context);
    }

    @GameTest(template = "empty", batch = "plantcompat", timeoutTicks = 40)
    public static void testHarvestBerriesFromMatureBush(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromMatureBush(context);
    }

    @GameTest(template = "empty", batch = "plantcompat", timeoutTicks = 40)
    public static void testHarvestBerriesFromImmatureBushDoesNothing(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromImmatureBushDoesNothing(context);
    }

    @GameTest(template = "empty", batch = "plantcompat", timeoutTicks = 40)
    public static void testHarvestBerriesFromCaveVines(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromCaveVines(context);
    }
}
