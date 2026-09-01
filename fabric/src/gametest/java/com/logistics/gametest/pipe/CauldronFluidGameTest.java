package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the cauldron fluid GameTests. Test logic lives in
 * {@link CauldronFluidGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class CauldronFluidGameTest {

    /** Lava dwells 60 ticks in a pipe before it may hop (tick delay 30 × the dwell multiplier), so allow for it. */
    @GameTest(maxTicks = 200)
    public void extractorDrainsLavaCauldron(GameTestHelper context) {
        CauldronFluidGameTestBody.extractorDrainsLavaCauldron(context);
    }

    @GameTest
    public void extractorDrainsWaterCauldron(GameTestHelper context) {
        CauldronFluidGameTestBody.extractorDrainsWaterCauldron(context);
    }

    @GameTest
    public void insertionPipeFillsCauldronWithWater(GameTestHelper context) {
        CauldronFluidGameTestBody.insertionPipeFillsCauldronWithWater(context);
    }

    /** As above: the seeded lava parcel is not ready to move until its 60-tick dwell elapses. */
    @GameTest(maxTicks = 200)
    public void insertionPipeFillsCauldronWithLava(GameTestHelper context) {
        CauldronFluidGameTestBody.insertionPipeFillsCauldronWithLava(context);
    }

    /** Pulling a whole cauldron level through a real pipe network takes many rate-sized hops. */
    @GameTest(maxTicks = 400)
    public void pipeNetworkFillsCauldronWithWater(GameTestHelper context) {
        CauldronFluidGameTestBody.pipeNetworkFillsCauldronWithWater(context);
    }
}
