package com.logistics.gametest.forestry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Gourd Farm GameTests. Test logic lives in
 * {@link GourdFarmGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class GourdFarmGameTest {

    @GameTest(maxTicks = 1200)
    public void testHarvestsFruitNeverStemsAndHopperPullsHarvest(GameTestHelper context) {
        GourdFarmGameTestBody.testHarvestsFruitNeverStemsAndHopperPullsHarvest(context);
    }

    @GameTest(maxTicks = 1200)
    public void testFullFarmSpillsAtTheStem(GameTestHelper context) {
        GourdFarmGameTestBody.testFullFarmSpillsAtTheStem(context);
    }

    @GameTest(maxTicks = 1200)
    public void testBrokenStemIsReTilledAndReplanted(GameTestHelper context) {
        GourdFarmGameTestBody.testBrokenStemIsReTilledAndReplanted(context);
    }
}
