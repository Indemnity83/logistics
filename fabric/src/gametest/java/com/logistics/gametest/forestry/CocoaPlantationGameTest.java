package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Cocoa Plantation GameTests. Test logic lives in
 * {@link CocoaPlantationGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class CocoaPlantationGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testPicksAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        CocoaPlantationGameTestBody.testPicksAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testHopperFedBeansReplantEmptyPlots(GameTestHelper context) {
        CocoaPlantationGameTestBody.testHopperFedBeansReplantEmptyPlots(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testPicksAndRefillsPodOutsideReachOnLogInside(GameTestHelper context) {
        CocoaPlantationGameTestBody.testPicksAndRefillsPodOutsideReachOnLogInside(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testDoesNothingWithoutPower(GameTestHelper context) {
        CocoaPlantationGameTestBody.testDoesNothingWithoutPower(context);
    }
}
