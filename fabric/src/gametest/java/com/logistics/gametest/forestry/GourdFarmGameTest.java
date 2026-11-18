package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Gourd Farm GameTests. Test logic lives in
 * {@link GourdFarmGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class GourdFarmGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testHarvestsFruitNeverStemsAndHopperPullsHarvest(GameTestHelper context) {
        GourdFarmGameTestBody.testHarvestsFruitNeverStemsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testFullFarmSpillsAtTheStem(GameTestHelper context) {
        GourdFarmGameTestBody.testFullFarmSpillsAtTheStem(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testBrokenStemIsReTilledAndReplanted(GameTestHelper context) {
        GourdFarmGameTestBody.testBrokenStemIsReTilledAndReplanted(context);
    }
}
