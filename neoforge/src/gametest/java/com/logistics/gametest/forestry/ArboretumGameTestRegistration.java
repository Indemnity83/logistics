package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link ArboretumGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class ArboretumGameTestRegistration {

    private ArboretumGameTestRegistration() {}

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 1200)
    public static void testPlantsSaplingsOnLattice(GameTestHelper context) {
        ArboretumGameTestBody.testPlantsSaplingsOnLattice(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 3000)
    public static void testFellsGrownTreeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsGrownTreeAndReplants(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 300)
    public static void testNoPowerPlantsNothing(GameTestHelper context) {
        ArboretumGameTestBody.testNoPowerPlantsNothing(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 300)
    public static void testRedstonePausesFarm(GameTestHelper context) {
        ArboretumGameTestBody.testRedstonePausesFarm(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 1200)
    public static void testClearsBlocksFromRing(GameTestHelper context) {
        ArboretumGameTestBody.testClearsBlocksFromRing(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 2000)
    public static void testHumusReplacesDirtAcrossTheRing(GameTestHelper context) {
        ArboretumGameTestBody.testHumusReplacesDirtAcrossTheRing(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 40)
    public static void testEveryGermlingGrowsOnArboretumSoils(GameTestHelper context) {
        ArboretumGameTestBody.testEveryGermlingGrowsOnArboretumSoils(context);
    }
}
