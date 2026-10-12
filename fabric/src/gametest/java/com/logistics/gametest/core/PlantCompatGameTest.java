package com.logistics.gametest.core;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the PlantCompat GameTests. Test logic lives in
 * {@link PlantCompatGameTestBody} (shared with NeoForge — see {@code common/src/gametest}).
 */
public class PlantCompatGameTest {

    @GameTest(maxTicks = 20)
    public void testFertilizeAdvancesWheat(GameTestHelper context) {
        PlantCompatGameTestBody.testFertilizeAdvancesWheat(context);
    }

    @GameTest(maxTicks = 20)
    public void testFertilizeRejectsNonBonemealableBlock(GameTestHelper context) {
        PlantCompatGameTestBody.testFertilizeRejectsNonBonemealableBlock(context);
    }

    @GameTest(maxTicks = 20)
    public void testHarvestBerriesFromMatureBush(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromMatureBush(context);
    }

    @GameTest(maxTicks = 20)
    public void testHarvestBerriesFromImmatureBushDoesNothing(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromImmatureBushDoesNothing(context);
    }

    @GameTest(maxTicks = 20)
    public void testHarvestBerriesFromCaveVines(GameTestHelper context) {
        PlantCompatGameTestBody.testHarvestBerriesFromCaveVines(context);
    }
}
