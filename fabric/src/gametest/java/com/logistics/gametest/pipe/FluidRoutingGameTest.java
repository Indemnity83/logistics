package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fluid-routing pipe GameTests. Test logic lives in
 * {@link FluidRoutingGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class FluidRoutingGameTest {

    @GameTest(maxTicks = 200)
    public void insertionPipesTopOffTanksInSequence(GameTestHelper context) {
        FluidRoutingGameTestBody.insertionPipesTopOffTanksInSequence(context);
    }

    @GameTest(maxTicks = 200)
    public void bypassPipeRunsPastAdjacentTank(GameTestHelper context) {
        FluidRoutingGameTestBody.bypassPipeRunsPastAdjacentTank(context);
    }

    @GameTest(maxTicks = 200)
    public void mergerPipeOutputsOnlyThroughWrenchSetFace(GameTestHelper context) {
        FluidRoutingGameTestBody.mergerPipeOutputsOnlyThroughWrenchSetFace(context);
    }

    @GameTest(maxTicks = 200)
    public void mergerPipeRefusesFluidThroughItsOutputFace(GameTestHelper context) {
        FluidRoutingGameTestBody.mergerPipeRefusesFluidThroughItsOutputFace(context);
    }

    @GameTest(maxTicks = 200)
    public void voidPipeDestroysFluidFlowingIntoIt(GameTestHelper context) {
        FluidRoutingGameTestBody.voidPipeDestroysFluidFlowingIntoIt(context);
    }
}
