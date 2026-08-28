package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the fluid pump GameTests. Test logic lives in
 * {@link FluidPumpGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class FluidPumpGameTest {

    @GameTest
    public void testFluidPumpPlacement(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpPlacement(context);
    }

    /**
     * Wiki claim (Power): "Supply it with RF from your power system — an engine, a Battery, or
     * Cables — to keep it draining." Faces other than the bottom (the world-facing intake) accept
     * both energy and fluid connections.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
     */
    @GameTest
    public void testFluidPumpEnergyAndTankAccessibleFromTopAndSides(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpEnergyAndTankAccessibleFromTopAndSides(context);
    }

    // NOTE: the wiki (Power section) says "with no power it stops," which reads as a blanket
    // statement. In practice only the fluid-draining step requires energy — the intake tube keeps
    // descending (searching for a source) with zero power, as this test confirms. See
    // testFluidPumpDoesNotDrainWithoutEnergy for the part of the claim that does hold: no power
    // means no fluid moves into the tank.
    @GameTest(maxTicks = 40)
    public void testFluidPumpTubeDescendsWithoutEnergy(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpTubeDescendsWithoutEnergy(context);
    }

    /**
     * Wiki claim (Usage): "The Pump drains fluid source blocks from the world below it into an
     * internal buffer..."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(maxTicks = 40)
    public void testFluidPumpRemovesSourceAndFillsTank(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpRemovesSourceAndFillsTank(context);
    }

    /**
     * Wiki claim (Power): "The Pump runs on RF and keeps its own internal buffer... with no power
     * it stops." An unpowered pump does not drain a source directly beneath its tube, even once the
     * tube reaches it.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Power">wiki/Pump.txt § Power</a>
     */
    @GameTest(maxTicks = 60)
    public void testFluidPumpDoesNotDrainWithoutEnergy(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDoesNotDrainWithoutEnergy(context);
    }

    @GameTest(maxTicks = 40)
    public void testFluidPumpDoesNotDrainWaterloggedBlocks(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDoesNotDrainWaterloggedBlocks(context);
    }

    @GameTest(maxTicks = 120)
    public void testFluidPumpFindsConnectedSourceInRadius(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpFindsConnectedSourceInRadius(context);
    }

    @GameTest(maxTicks = 160)
    public void testFluidPumpDrainsFinitePool(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsFinitePool(context);
    }

    @GameTest(maxTicks = 60)
    public void testFluidPumpTreatsLargeBodyAsInfinite(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpTreatsLargeBodyAsInfinite(context);
    }

    /**
     * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe." Setup step 3: "Place
     * a Glass Tank or Copper Fluid Pipe against the output."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void testFluidPumpOutputsToPipeAbove(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpOutputsToPipeAbove(context);
    }

    /**
     * Wiki claim (Usage): "...feeds them into an adjacent tank or fluid pipe" — the output is not
     * limited to a single face; a pipe on any non-bottom side works.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @GameTest(maxTicks = 60)
    public void testFluidPumpOutputsToPipeOnSide(GameTestHelper context) {
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
    @GameTest(maxTicks = 20)
    public void testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpPushRateIsFourHundredNotSixtyTwoPointFive(context);
    }

    @GameTest(maxTicks = 80)
    public void testFluidPumpDrainsConnectedLavaSources(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsConnectedLavaSources(context);
    }

    @GameTest(maxTicks = 80)
    public void testFluidPumpCrossesFlowingToReachSources(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpCrossesFlowingToReachSources(context);
    }

    @GameTest(maxTicks = 200)
    public void testFluidPumpDrainsOpenLavaPool(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsOpenLavaPool(context);
    }

    @GameTest(maxTicks = 80)
    public void testFluidPumpFinishesLayerWithOutputTank(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpFinishesLayerWithOutputTank(context);
    }

    @GameTest(maxTicks = 60)
    public void testFluidPumpStallsAboveSolidFloor(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpStallsAboveSolidFloor(context);
    }

    @GameTest(maxTicks = 40)
    public void testFluidPumpDrainsFurthestFirst(GameTestHelper context) {
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
    @GameTest(maxTicks = 100)
    public void testFluidPumpDrainsAndOutputsViaRealEngine(GameTestHelper context) {
        FluidPumpGameTestBody.testFluidPumpDrainsAndOutputsViaRealEngine(context);
    }
}
