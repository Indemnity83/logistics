package com.logistics.gametest.forestry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Humus GameTests. Test logic lives in
 * {@link HumusGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class HumusGameTest {

    @GameTest(maxTicks = 40)
    public void testSaplingSurvivesOnHumus(GameTestHelper context) {
        HumusGameTestBody.testSaplingSurvivesOnHumus(context);
    }

    @GameTest(maxTicks = 40)
    public void testHumusBoostGrowsTreeAndTurnsToSand(GameTestHelper context) {
        HumusGameTestBody.testHumusBoostGrowsTreeAndTurnsToSand(context);
    }

    @GameTest(maxTicks = 40)
    public void testHumusUnderLogTurnsToSand(GameTestHelper context) {
        HumusGameTestBody.testHumusUnderLogTurnsToSand(context);
    }

    @GameTest(maxTicks = 40)
    public void testHumusWithoutSaplingNeverChanges(GameTestHelper context) {
        HumusGameTestBody.testHumusWithoutSaplingNeverChanges(context);
    }

    @GameTest(maxTicks = 40)
    public void testNoGrowthInDarkness(GameTestHelper context) {
        HumusGameTestBody.testNoGrowthInDarkness(context);
    }
}
