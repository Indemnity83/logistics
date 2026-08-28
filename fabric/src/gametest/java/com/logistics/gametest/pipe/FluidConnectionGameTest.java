package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fluid connection GameTests. Test logic lives in
 * {@link FluidConnectionGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class FluidConnectionGameTest {

    /**
    * Two fluid extractors placed side by side must not connect to each other — each extractor is an
    * independent endpoint that only reaches its source handler and downstream transport pipes.
    * Regression test for #676.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void fluidExtractorsDoNotConnectToEachOther(GameTestHelper context) {
        FluidConnectionGameTestBody.fluidExtractorsDoNotConnectToEachOther(context);
    }

    /**
    * A fluid extractor still connects to a downstream transport pipe (positive control for the
    * connection veto above).
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void fluidExtractorConnectsToTransportPipe(GameTestHelper context) {
        FluidConnectionGameTestBody.fluidExtractorConnectsToTransportPipe(context);
    }
}
