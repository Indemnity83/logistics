package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Humus GameTests. Test logic lives in
 * {@link HumusGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class HumusGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testSaplingSurvivesOnHumus(GameTestHelper context) {
        HumusGameTestBody.testSaplingSurvivesOnHumus(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testHumusGrowsSaplingIntoTree(GameTestHelper context) {
        HumusGameTestBody.testHumusGrowsSaplingIntoTree(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testHumusWithoutSaplingNeverChanges(GameTestHelper context) {
        HumusGameTestBody.testHumusWithoutSaplingNeverChanges(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testNoGrowthInDarkness(GameTestHelper context) {
        HumusGameTestBody.testNoGrowthInDarkness(context);
    }
}
