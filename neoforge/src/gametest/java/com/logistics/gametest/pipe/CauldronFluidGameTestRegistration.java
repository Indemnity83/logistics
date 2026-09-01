package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CauldronFluidGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CauldronFluidGameTestRegistration {

    private CauldronFluidGameTestRegistration() {}

    /** Lava dwells 60 ticks in a pipe before it may hop (tick delay 30 × the dwell multiplier), so allow for it. */
    @GameTest(template = "empty", batch = "cauldronfluid", timeoutTicks = 200)
    public static void extractorDrainsLavaCauldron(GameTestHelper context) {
        CauldronFluidGameTestBody.extractorDrainsLavaCauldron(context);
    }

    @GameTest(template = "empty", batch = "cauldronfluid", timeoutTicks = 100)
    public static void extractorDrainsWaterCauldron(GameTestHelper context) {
        CauldronFluidGameTestBody.extractorDrainsWaterCauldron(context);
    }

    @GameTest(template = "empty", batch = "cauldronfluid", timeoutTicks = 100)
    public static void insertionPipeFillsCauldronWithWater(GameTestHelper context) {
        CauldronFluidGameTestBody.insertionPipeFillsCauldronWithWater(context);
    }

    /** As above: the seeded lava parcel is not ready to move until its 60-tick dwell elapses. */
    @GameTest(template = "empty", batch = "cauldronfluid", timeoutTicks = 200)
    public static void insertionPipeFillsCauldronWithLava(GameTestHelper context) {
        CauldronFluidGameTestBody.insertionPipeFillsCauldronWithLava(context);
    }

    /** Pulling a whole cauldron level through a real pipe network takes many rate-sized hops. */
    @GameTest(template = "empty", batch = "cauldronfluid", timeoutTicks = 400)
    public static void pipeNetworkFillsCauldronWithWater(GameTestHelper context) {
        CauldronFluidGameTestBody.pipeNetworkFillsCauldronWithWater(context);
    }
}
