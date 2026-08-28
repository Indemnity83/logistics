package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidConnectionGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidConnectionGameTestRegistration {

    private FluidConnectionGameTestRegistration() {}

    /**
    * Two fluid extractors placed side by side must not connect to each other — each extractor is an
    * independent endpoint that only reaches its source handler and downstream transport pipes.
    * Regression test for #676.
    */
    @GameTest(template = "empty", batch = "fluidconnection", timeoutTicks = 40)
    public static void fluidExtractorsDoNotConnectToEachOther(GameTestHelper context) {
        FluidConnectionGameTestBody.fluidExtractorsDoNotConnectToEachOther(context);
    }

    /**
    * A fluid extractor still connects to a downstream transport pipe (positive control for the
    * connection veto above).
    */
    @GameTest(template = "empty", batch = "fluidconnection", timeoutTicks = 40)
    public static void fluidExtractorConnectsToTransportPipe(GameTestHelper context) {
        FluidConnectionGameTestBody.fluidExtractorConnectsToTransportPipe(context);
    }
}
