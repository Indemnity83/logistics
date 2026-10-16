package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the machine redstone-control GameTests. Test logic lives in
 * {@link RedstoneControlGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class RedstoneControlGameTest {

    @GameTest(maxTicks = 160)
    public void testIgnoredRunsWithAndWithoutSignal(GameTestHelper context) {
        RedstoneControlGameTestBody.testIgnoredRunsWithAndWithoutSignal(context);
    }

    @GameTest(maxTicks = 200)
    public void testHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testHighPausesUntilSignalled(context);
    }

    @GameTest(maxTicks = 200)
    public void testLowPausesWhileSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testLowPausesWhileSignalled(context);
    }

    @GameTest(maxTicks = 220)
    public void testModeChangeMidRunFreezesProgress(GameTestHelper context) {
        RedstoneControlGameTestBody.testModeChangeMidRunFreezesProgress(context);
    }

    @GameTest(maxTicks = 260)
    public void testLeverTogglesPauseAndResume(GameTestHelper context) {
        RedstoneControlGameTestBody.testLeverTogglesPauseAndResume(context);
    }

    @GameTest(maxTicks = 240)
    public void testFabricatorHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testFabricatorHighPausesUntilSignalled(context);
    }

    @GameTest(maxTicks = 140)
    public void testTransposerHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testTransposerHighPausesUntilSignalled(context);
    }

    @GameTest
    public void testModePersistsAndDefaultIsNotSaved(GameTestHelper context) {
        RedstoneControlGameTestBody.testModePersistsAndDefaultIsNotSaved(context);
    }
}
