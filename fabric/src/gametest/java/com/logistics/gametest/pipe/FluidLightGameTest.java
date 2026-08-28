package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fluid light GameTests. Test logic lives in
 * {@link FluidLightGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class FluidLightGameTest {

    /** A tank of lava glows at 15. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfLavaGlows(GameTestHelper context) {
        FluidLightGameTestBody.tankOfLavaGlows(context);
    }

    /** A tank of a glowing custom fluid glows at its luminance. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfLiquidGlowstoneGlows(GameTestHelper context) {
        FluidLightGameTestBody.tankOfLiquidGlowstoneGlows(context);
    }

    /** A tank of a non-glowing fluid stays dark. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfWaterStaysDark(GameTestHelper context) {
        FluidLightGameTestBody.tankOfWaterStaysDark(context);
    }

    /** Draining a glowing tank turns its light back off. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void drainedTankStopsGlowing(GameTestHelper context) {
        FluidLightGameTestBody.drainedTankStopsGlowing(context);
    }

    /** A pipe carrying a glowing fluid emits its light. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void pipeOfLavaGlows(GameTestHelper context) {
        FluidLightGameTestBody.pipeOfLavaGlows(context);
    }
}
