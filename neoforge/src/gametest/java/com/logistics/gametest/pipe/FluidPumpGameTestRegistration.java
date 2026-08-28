package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidPumpGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidPumpGameTestRegistration {

    private FluidPumpGameTestRegistration() {}

    @GameTest(template = "empty", batch = "fluidpump")
    public static void testFluidPumpPlacement(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpPlacement(context);
    }

    /**
    * Wiki claim (Power): "Supply it with RF from your power system — an engine, a Battery, or
    * Cables — to keep it draining." Faces other than the bottom (the world-facing intake) accept
    * both energy and fluid connections.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
    */
    @GameTest(template = "empty", batch = "fluidpump")
    public static void testFluidPumpEnergyAndTankAccessibleFromTopAndSides(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpEnergyAndTankAccessibleFromTopAndSides(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 40)
    public static void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpTubeDescendsWithoutEnergy(context);
    }

    /**
    * Wiki claim (Usage): "The Pump drains fluid source blocks from the world below it into an
    * internal buffer..."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 40)
    public static void testFluidPumpRemovesSourceAndFillsTank(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpRemovesSourceAndFillsTank(context);
    }

    /**
    * Wiki claim (Power): "The Pump runs on RF and keeps its own internal buffer... with no power
    * it stops." An unpowered pump does not drain a source directly beneath its tube, even once the
    * tube reaches it.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 60)
    public static void testFluidPumpDoesNotDrainWithoutEnergy(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDoesNotDrainWithoutEnergy(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 40)
    public static void testFluidPumpDoesNotDrainWaterloggedBlocks(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDoesNotDrainWaterloggedBlocks(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 120)
    public static void testFluidPumpFindsConnectedSourceInRadius(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpFindsConnectedSourceInRadius(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 160)
    public static void testFluidPumpDrainsFinitePool(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsFinitePool(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 60)
    public static void testFluidPumpTreatsLargeBodyAsInfinite(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpTreatsLargeBodyAsInfinite(context);
    }

    /**
    * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe." Setup step 3: "Place
    * a Glass Tank or Copper Fluid Pipe against the output."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 60)
    public static void testFluidPumpOutputsToPipeAbove(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpOutputsToPipeAbove(context);
    }

    /**
    * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe" — the output is not
    * limited to a single face; a pipe on any non-bottom side works.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 60)
    public static void testFluidPumpOutputsToPipeOnSide(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpOutputsToPipeOnSide(context);
    }

    /**
    * Wiki claim (Usage): "Outputs up to 62.5 mB/t into an adjacent tank or fluid pipe."
    *
    * <p>NOTE: the code's push-rate constant (FLUID_PUMP_PUSH_RATE_MB) is 400 mB/tick, not 62.5.
    * 62.5 mB/t (= 1000 mB ÷ FLUID_PUMP_INTERVAL_TICKS(16)) looks like the *sustained average intake
    * rate* (one bucket drained from the world every 16 ticks), mislabeled here as the output/push
    * rate. This test asserts the push path directly — bypassing draining by preloading the pump's
    * own tank — and confirms one tick moves exactly FLUID_PUMP_PUSH_RATE_MB, not ~62.5. See
    * WIKI_DISCREPANCIES.md § Pump.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 20)
    public static void testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 80)
    public static void testFluidPumpDrainsConnectedLavaSources(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsConnectedLavaSources(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 80)
    public static void testFluidPumpCrossesFlowingToReachSources(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpCrossesFlowingToReachSources(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 200)
    public static void testFluidPumpDrainsOpenLavaPool(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsOpenLavaPool(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 80)
    public static void testFluidPumpFinishesLayerWithOutputTank(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpFinishesLayerWithOutputTank(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 60)
    public static void testFluidPumpStallsAboveSolidFloor(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpStallsAboveSolidFloor(context);
    }

    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 40)
    public static void testFluidPumpDrainsFurthestFirst(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsFurthestFirst(context);
    }

    /**
    * Wiki claim (Power/Usage): "Supply it with RF from your power system... to keep it draining,"
    * feeding "into an adjacent tank." The tests above prove draining and push-rate math by
    * inserting energy directly; this one proves the whole feature as a player wires it up — a real
    * engine (no cable) delivering power and a real Glass Tank receiving the output.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
    * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "fluidpump", timeoutTicks = 100)
    public static void testFluidPumpDrainsAndOutputsViaRealEngine(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsAndOutputsViaRealEngine(context);
    }
}
