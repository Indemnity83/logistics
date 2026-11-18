package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Plantation GameTests. Test logic lives in
 * {@link PlantationGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class PlantationGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 600)
    public void testHarvestsStalksAndKeepsEveryBase(GameTestHelper context) {
        PlantationGameTestBody.testHarvestsStalksAndKeepsEveryBase(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testFullFarmSpillsAtTheBase(GameTestHelper context) {
        PlantationGameTestBody.testFullFarmSpillsAtTheBase(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testReplantsAMissingBaseFromTheInventory(GameTestHelper context) {
        PlantationGameTestBody.testReplantsAMissingBaseFromTheInventory(context);
    }
}
