package com.logistics.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the harness-contract GameTests. Test logic lives in
 * {@link HarnessGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); this
 * method only carries the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class HarnessGameTest {

    @GameTest
    public void testStructureCoversTheSharedFootprint(GameTestHelper context) {
        HarnessGameTestBody.testStructureCoversTheSharedFootprint(context);
    }
}
