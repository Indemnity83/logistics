package com.logistics.gametest.network;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Crafting, Process and Satellite Logistics Pipe GameTests. Test
 * logic lives in {@link CraftingProcessSatelliteGameTestBody} (shared with NeoForge — see
 * {@code common/src/gametest}).
 *
 * <p>Run one test: /test run logistics-gametest.craftingprocesssatellitegametest.&lt;methodname&gt;
 */
public class CraftingProcessSatelliteGameTest {

    /** A crafting pipe crafts a requested item from ingredients a provider holds. */
    @GameTest(maxTicks = 300)
    public void testCraftingPipeCraftsRequestedItemFromProvidedIngredients(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testCraftingPipeCraftsRequestedItemFromProvidedIngredients(context);
    }

    /** Two chained crafting pipes: the second consumes what the first produces. */
    @GameTest(maxTicks = 400)
    public void testChainedCraftingPipesConsumeEachOthersOutput(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testChainedCraftingPipesConsumeEachOthersOutput(context);
    }

    /** Chained crafting pipes supply an intermediate used in several grid slots. */
    @GameTest(maxTicks = 400)
    public void testChainedCraftingPipesSupplyMultiSlotIntermediate(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testChainedCraftingPipesSupplyMultiSlotIntermediate(context);
    }

    /** A process pipe smelts through a blast furnace whose input is fed by a satellite. */
    @GameTest(maxTicks = 500)
    public void testProcessPipeSmeltsThroughSatelliteFedFurnace(GameTestHelper context) {
        CraftingProcessSatelliteGameTestBody.testProcessPipeSmeltsThroughSatelliteFedFurnace(context);
    }
}
