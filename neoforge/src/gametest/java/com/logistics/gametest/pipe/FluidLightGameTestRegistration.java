package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidLightGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidLightGameTestRegistration {

    private FluidLightGameTestRegistration() {}

    /** A tank of lava glows at 15. */
    @GameTest(template = "empty", batch = "fluidlight", timeoutTicks = 40)
    public static void tankOfLavaGlows(GameTestHelper context) {
        FluidLightGameTestBody.tankOfLavaGlows(context);
    }

    /** A tank of a glowing custom fluid glows at its luminance. */
    @GameTest(template = "empty", batch = "fluidlight", timeoutTicks = 40)
    public static void tankOfLiquidGlowstoneGlows(GameTestHelper context) {
        FluidLightGameTestBody.tankOfLiquidGlowstoneGlows(context);
    }

    /** A tank of a non-glowing fluid stays dark. */
    @GameTest(template = "empty", batch = "fluidlight", timeoutTicks = 40)
    public static void tankOfWaterStaysDark(GameTestHelper context) {
        FluidLightGameTestBody.tankOfWaterStaysDark(context);
    }

    /** Draining a glowing tank turns its light back off. */
    @GameTest(template = "empty", batch = "fluidlight", timeoutTicks = 40)
    public static void drainedTankStopsGlowing(GameTestHelper context) {
        FluidLightGameTestBody.drainedTankStopsGlowing(context);
    }

    /** A pipe carrying a glowing fluid emits its light. */
    @GameTest(template = "empty", batch = "fluidlight", timeoutTicks = 40)
    public static void pipeOfLavaGlows(GameTestHelper context) {
        FluidLightGameTestBody.pipeOfLavaGlows(context);
    }
}
