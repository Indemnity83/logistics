package com.logistics.gametest.forestry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Tree Feller GameTests. Test logic lives in
 * {@link TreeFellerGameTestBody} (shared with NeoForge — see {@code common/src/gametest}).
 */
public class TreeFellerGameTest {

    @GameTest
    public void testLevelTreeViewClassifiesAndPlansATree(GameTestHelper context) {
        TreeFellerGameTestBody.testLevelTreeViewClassifiesAndPlansATree(context);
    }
}
