package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the machine-controls GameTests. Test logic lives in
 * {@link MachineControlsGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class MachineControlsGameTest {

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testEveryServerMenuCarriesControls(GameTestHelper context) {
        MachineControlsGameTestBody.testEveryServerMenuCarriesControls(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMaceratorMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorMenuHandlerValidation(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testCropFarmMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testCropFarmMenuHandlerValidation(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMagmaticEngineMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticEngineMenuHandlerValidation(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testMaceratorRedstoneButtonPacketPath(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorRedstoneButtonPacketPath(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 280)
    public void testMaceratorUsageTracksProcessing(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorUsageTracksProcessing(context);
    }

    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testMagmaticGenerationWithinMax(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticGenerationWithinMax(context);
    }
}
