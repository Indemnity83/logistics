package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Orchard GameTests. Test logic lives in {@link OrchardGameTestBody}
 * (shared with NeoForge — see {@code common/src/gametest}); these methods only carry the
 * {@code @GameTest} annotation Fabric's reflection-based test discovery requires.
 */
public class OrchardGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 1200)
    public void testPicksApplesWithoutBreakingTheTree(GameTestHelper context) {
        OrchardGameTestBody.testPicksApplesWithoutBreakingTheTree(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testPicksNothingWithoutPower(GameTestHelper context) {
        OrchardGameTestBody.testPicksNothingWithoutPower(context);
    }
}
