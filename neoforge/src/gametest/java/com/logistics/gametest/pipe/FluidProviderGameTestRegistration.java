package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FluidProviderGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FluidProviderGameTestRegistration {

    private FluidProviderGameTestRegistration() {}

    /** Delivers N max-size packets and charges the configured energy for each one. */
    @GameTest(template = "empty", batch = "fluidprovider", timeoutTicks = 160)
    public static void testFluidProviderMintsAndDeliversPackets(GameTestHelper context) {
        FluidProviderGameTestBody.testFluidProviderMintsAndDeliversPackets(context);
    }

    /**
    * No minimum: a tank holding less than one max-size packet's worth still dispatches — its full
    * remainder ships as a single, correctly-sized packet, draining the tank to zero.
    */
    @GameTest(template = "empty", batch = "fluidprovider", timeoutTicks = 120)
    public static void testFluidProviderDispatchesBelowOnePacketSize(GameTestHelper context) {
        FluidProviderGameTestBody.testFluidProviderDispatchesBelowOnePacketSize(context);
    }

    /**
    * Mixed full-plus-tail dispatch charges exactly one endpoint cost per physical packet: a dispatch
    * producing 2 full-size packets + 1 tail packet (3 physical packets) must charge exactly {@code 3 *
    * rf}, and mint exactly three distinct, single-count item stacks (never combined into one stack).
    */
    @GameTest(template = "empty", batch = "fluidprovider", timeoutTicks = 160)
    public static void testMixedFullPlusTailDispatchChargesPerPhysicalPacket(GameTestHelper context) {
        FluidProviderGameTestBody.testMixedFullPlusTailDispatchChargesPerPhysicalPacket(context);
    }

    /**
    * Flat per-physical-packet energy cost, not per-mB: one dispatch of a full-size packet costs one
    * endpoint charge; ten independently-dispatched small deliveries of the same total volume cost ten
    * — proving cost tracks physical packet events, not aggregate mB moved.
    */
    @GameTest(template = "empty", batch = "fluidprovider", timeoutTicks = 300)
    public static void testTenSmallDeliveriesCostTenTimesOneBigDelivery(GameTestHelper context) {
        FluidProviderGameTestBody.testTenSmallDeliveriesCostTenTimesOneBigDelivery(context);
    }
}
