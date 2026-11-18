package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Nether Wart Farm GameTests. Test logic lives in
 * {@link NetherWartFarmGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class NetherWartFarmGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        NetherWartFarmGameTestBody.testHarvestsAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testRefillsEmptyPlotFromHopperFedWart(GameTestHelper context) {
        NetherWartFarmGameTestBody.testRefillsEmptyPlotFromHopperFedWart(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        NetherWartFarmGameTestBody.testFullFarmSpillsAtThePlot(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        NetherWartFarmGameTestBody.testDoesNothingWithoutPower(context);
    }
}
