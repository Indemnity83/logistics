package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the machine redstone-control GameTests. Test logic lives in
 * {@link RedstoneControlGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class RedstoneControlGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 160)
    public void testIgnoredRunsWithAndWithoutSignal(GameTestHelper context) {
        RedstoneControlGameTestBody.testIgnoredRunsWithAndWithoutSignal(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testHighPausesUntilSignalled(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testLowPausesWhileSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testLowPausesWhileSignalled(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 220)
    public void testModeChangeMidRunFreezesProgress(GameTestHelper context) {
        RedstoneControlGameTestBody.testModeChangeMidRunFreezesProgress(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testLeverTogglesPauseAndResume(GameTestHelper context) {
        RedstoneControlGameTestBody.testLeverTogglesPauseAndResume(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 240)
    public void testFabricatorHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testFabricatorHighPausesUntilSignalled(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 140)
    public void testTransposerHighPausesUntilSignalled(GameTestHelper context) {
        RedstoneControlGameTestBody.testTransposerHighPausesUntilSignalled(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testModePersistsAndDefaultIsNotSaved(GameTestHelper context) {
        RedstoneControlGameTestBody.testModePersistsAndDefaultIsNotSaved(context);
    }
}
