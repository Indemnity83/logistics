package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidPacketDropGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidPacketDropGameTestRegistration {

    private FluidPacketDropGameTestRegistration() {}

    /**
    * A fluid packet stranded in a broken pipe must not spawn as a ground item — it's voided instead.
    */
    @GameTest(template = "empty", batch = "fluidpacketdrop")
    public static void testFluidPacketNeverDropsOnPipeBreak(GameTestHelper context) {
        FluidPacketDropGameTestBody.testFluidPacketNeverDropsOnPipeBreak(context);
    }

    /**
    * Control: a normal item stranded the same way must still drop as usual.
    */
    @GameTest(template = "empty", batch = "fluidpacketdrop")
    public static void testNormalItemStillDropsOnPipeBreak(GameTestHelper context) {
        FluidPacketDropGameTestBody.testNormalItemStillDropsOnPipeBreak(context);
    }
}
