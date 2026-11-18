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

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 2400)
    public static void testFellsHumusTreeWholeAndReplants(GameTestHelper context) {
        ArboretumGameTestBody.testFellsHumusTreeWholeAndReplants(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 600)
    public static void testLeavesTreeOnGrassStanding(GameTestHelper context) {
        ArboretumGameTestBody.testLeavesTreeOnGrassStanding(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 400)
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        ArboretumGameTestBody.testDoesNothingWithoutPower(context);
    }

    @GameTest(template = "empty", batch = "arboretum", timeoutTicks = 400)
    public static void testRedstoneSignalPausesFarm(GameTestHelper context) {
        ArboretumGameTestBody.testRedstoneSignalPausesFarm(context);
    }
}
