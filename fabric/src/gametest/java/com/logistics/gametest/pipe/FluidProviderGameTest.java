package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Fluid Provider Pipe GameTests. Test logic lives in
 * {@link FluidProviderGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Run in-game: /test run logistics-gametest.fluidprovidergametest.&lt;methodname&gt;
 */
public class FluidProviderGameTest {

    /** Delivers N max-size packets and charges the configured energy for each one. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 160)
    public void testFluidProviderMintsAndDeliversPackets(GameTestHelper context) {
        FluidProviderGameTestBody.testFluidProviderMintsAndDeliversPackets(context);
    }

    /**
    * No minimum: a tank holding less than one max-size packet's worth still dispatches — its full
    * remainder ships as a single, correctly-sized packet, draining the tank to zero.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 120)
    public void testFluidProviderDispatchesBelowOnePacketSize(GameTestHelper context) {
        FluidProviderGameTestBody.testFluidProviderDispatchesBelowOnePacketSize(context);
    }

    /**
    * Mixed full-plus-tail dispatch charges exactly one endpoint cost per physical packet: a dispatch
    * producing 2 full-size packets + 1 tail packet (3 physical packets) must charge exactly {@code 3 *
    * rf}, and mint exactly three distinct, single-count item stacks (never combined into one stack).
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 160)
    public void testMixedFullPlusTailDispatchChargesPerPhysicalPacket(GameTestHelper context) {
        FluidProviderGameTestBody.testMixedFullPlusTailDispatchChargesPerPhysicalPacket(context);
    }

    /**
    * Flat per-physical-packet energy cost, not per-mB: one dispatch of a full-size packet costs one
    * endpoint charge; ten independently-dispatched small deliveries of the same total volume cost ten
    * — proving cost tracks physical packet events, not aggregate mB moved.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 300)
    public void testTenSmallDeliveriesCostTenTimesOneBigDelivery(GameTestHelper context) {
        FluidProviderGameTestBody.testTenSmallDeliveriesCostTenTimesOneBigDelivery(context);
    }
}
