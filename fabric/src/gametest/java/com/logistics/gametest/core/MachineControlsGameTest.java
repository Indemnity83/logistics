package com.logistics.gametest.core;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the machine-controls GameTests. Test logic lives in
 * {@link MachineControlsGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class MachineControlsGameTest {

    @GameTest
    public void testEveryServerMenuCarriesControls(GameTestHelper context) {
        MachineControlsGameTestBody.testEveryServerMenuCarriesControls(context);
    }

    @GameTest
    public void testMaceratorMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorMenuHandlerValidation(context);
    }

    @GameTest
    public void testCropFarmMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testCropFarmMenuHandlerValidation(context);
    }

    @GameTest
    public void testMagmaticEngineMenuHandlerValidation(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticEngineMenuHandlerValidation(context);
    }

    @GameTest
    public void testMaceratorRedstoneButtonPacketPath(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorRedstoneButtonPacketPath(context);
    }

    @GameTest(maxTicks = 280)
    public void testMaceratorUsageTracksProcessing(GameTestHelper context) {
        MachineControlsGameTestBody.testMaceratorUsageTracksProcessing(context);
    }

    @GameTest(maxTicks = 200)
    public void testMagmaticGenerationWithinMax(GameTestHelper context) {
        MachineControlsGameTestBody.testMagmaticGenerationWithinMax(context);
    }
}
