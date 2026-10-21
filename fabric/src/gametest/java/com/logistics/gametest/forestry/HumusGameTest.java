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
    public void testOakGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testOakGrowsSmallTree(context);
    }

    @GameTest(maxTicks = 40)
    public void testDarkOakGrowsFromOneSapling(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGrowsFromOneSapling(context);
    }

    @GameTest(maxTicks = 40)
    public void testAzaleaGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testAzaleaGrowsSmallTree(context);
    }

    @GameTest(maxTicks = 40)
    public void testMangroveGrowsSmallTreeWithGrownPropagules(GameTestHelper context) {
        HumusGameTestBody.testMangroveGrowsSmallTreeWithGrownPropagules(context);
    }

    @GameTest(maxTicks = 40)
    public void testDarkOakGroupGrowsFourTrees(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGroupGrowsFourTrees(context);
    }

    @GameTest(maxTicks = 40)
    public void testSaplingsOffHumusGrowAsVanilla(GameTestHelper context) {
        HumusGameTestBody.testSaplingsOffHumusGrowAsVanilla(context);
    }

    @GameTest(maxTicks = 40)
    public void testHumusIsPermanentAndGivesNoBoost(GameTestHelper context) {
        HumusGameTestBody.testHumusIsPermanentAndGivesNoBoost(context);
    }
}
