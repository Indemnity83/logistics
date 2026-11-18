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
    public void testOakGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testOakGrowsSmallTree(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testDarkOakGrowsFromOneSapling(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGrowsFromOneSapling(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testSpruceGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testSpruceGrowsSmallTree(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testAzaleaGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testAzaleaGrowsSmallTree(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testMangroveGrowsSmallTreeWithGrownPropagules(GameTestHelper context) {
        HumusGameTestBody.testMangroveGrowsSmallTreeWithGrownPropagules(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testDarkOakGroupGrowsFourTrees(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGroupGrowsFourTrees(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testSpruceGroupGrowsFourTrees(GameTestHelper context) {
        HumusGameTestBody.testSpruceGroupGrowsFourTrees(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testSaplingsOffHumusGrowAsVanilla(GameTestHelper context) {
        HumusGameTestBody.testSaplingsOffHumusGrowAsVanilla(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testHumusIsPermanentAndGivesNoBoost(GameTestHelper context) {
        HumusGameTestBody.testHumusIsPermanentAndGivesNoBoost(context);
    }
}
