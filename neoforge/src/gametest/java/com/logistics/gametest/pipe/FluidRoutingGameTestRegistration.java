package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidRoutingGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidRoutingGameTestRegistration {

    private FluidRoutingGameTestRegistration() {}

    @GameTest(template = "empty", batch = "fluidrouting", timeoutTicks = 220)
    public static void insertionPipesTopOffTanksInSequence(GameTestHelper context) {
        FluidRoutingGameTestBody.insertionPipesTopOffTanksInSequence(context);
    }

    @GameTest(template = "empty", batch = "fluidrouting", timeoutTicks = 220)
    public static void bypassPipeRunsPastAdjacentTank(GameTestHelper context) {
        FluidRoutingGameTestBody.bypassPipeRunsPastAdjacentTank(context);
    }

    @GameTest(template = "empty", batch = "fluidrouting", timeoutTicks = 220)
    public static void mergerPipeOutputsOnlyThroughWrenchSetFace(GameTestHelper context) {
        FluidRoutingGameTestBody.mergerPipeOutputsOnlyThroughWrenchSetFace(context);
    }

    @GameTest(template = "empty", batch = "fluidrouting", timeoutTicks = 220)
    public static void mergerPipeRefusesFluidThroughItsOutputFace(GameTestHelper context) {
        FluidRoutingGameTestBody.mergerPipeRefusesFluidThroughItsOutputFace(context);
    }

    @GameTest(template = "empty", batch = "fluidrouting", timeoutTicks = 220)
    public static void voidPipeDestroysFluidFlowingIntoIt(GameTestHelper context) {
        FluidRoutingGameTestBody.voidPipeDestroysFluidFlowingIntoIt(context);
    }
}
