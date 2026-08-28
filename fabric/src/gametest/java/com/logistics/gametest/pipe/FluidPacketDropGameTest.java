package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fluid packet drop GameTests. Test logic lives in
 * {@link FluidPacketDropGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 *
 * <p>Run in-game: /test run logistics-gametest.fluidpacketdropgametest.&lt;methodname&gt;
 */
public class FluidPacketDropGameTest {

    /**
     * A fluid packet stranded in a broken pipe must not spawn as a ground item — it's voided instead.
     */
    @GameTest
    public void testFluidPacketNeverDropsOnPipeBreak(GameTestHelper context) {
        FluidPacketDropGameTestBody.testFluidPacketNeverDropsOnPipeBreak(context);
    }

    /**
     * Control: a normal item stranded the same way must still drop as usual.
     */
    @GameTest
    public void testNormalItemStillDropsOnPipeBreak(GameTestHelper context) {
        FluidPacketDropGameTestBody.testNormalItemStillDropsOnPipeBreak(context);
    }
}
