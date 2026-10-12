package com.logistics.gametest.network;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CraftingProcessSatelliteGameTestBody}'s methods into NeoForge's GameTest discovery —
 * see {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CraftingProcessSatelliteGameTestRegistration {

    private CraftingProcessSatelliteGameTestRegistration() {}

    @GameTest(template = "empty", batch = "craftingprocesssatellite", timeoutTicks = 320)
    public static void testCraftingPipeCraftsRequestedItemFromProvidedIngredients(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testCraftingPipeCraftsRequestedItemFromProvidedIngredients(context);
    }

    @GameTest(template = "empty", batch = "craftingprocesssatellite", timeoutTicks = 420)
    public static void testChainedCraftingPipesConsumeEachOthersOutput(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testChainedCraftingPipesConsumeEachOthersOutput(context);
    }

    @GameTest(template = "empty", batch = "craftingprocesssatellite", timeoutTicks = 520)
    public static void testProcessPipeSmeltsThroughSatelliteFedFurnace(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testProcessPipeSmeltsThroughSatelliteFedFurnace(context);
    }
}
