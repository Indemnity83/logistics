package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Fluid Supplier Pipe GameTests. Test logic lives in
 * {@link FluidSupplierGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Run in-game: /test run logistics-gametest.fluidsuppliergametest.&lt;methodname&gt;
 */
public class FluidSupplierGameTest {

    /**
    * Exact fill, no rounding: the supplier orders exactly the deficit (not rounded up to any packet
    * multiple), fills the machine tank to exactly the target, and leaves an empty buffer. This
    * directly replaces the old quantum-rounding behavior (packets no longer have a minimum size).
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testDryRestockFillsToTarget(GameTestHelper context) {
        FluidSupplierGameTestBody.testDryRestockFillsToTarget(context);
    }

    /**
    * A non-round deficit (e.g. 137 mB) is ordered and fulfilled exactly — proving "no minimum packet
    * size" end to end at the consumer, not just rounded down to the nearest packet multiple.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testExactNonRoundDeficitIsOrderedAndFilled(GameTestHelper context) {
        FluidSupplierGameTestBody.testExactNonRoundDeficitIsOrderedAndFilled(context);
    }

    /**
    * No over-reserve: across many ticks the supplier never has more fluid held plus on order than its
    * buffer can hold (held + pending ≤ capacity). Target exceeds a single batch so ordering stays busy.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testNeverOrdersMoreThanBufferCanHold(GameTestHelper context) {
        FluidSupplierGameTestBody.testNeverOrdersMoreThanBufferCanHold(context);
    }

    /**
    * Insufficient energy: the network holds only enough energy for the provider to mint packets, none
    * left for the supplier to "pay" for them. Delivered packets are accepted into the buffer but no
    * fluid reaches the machine tank. Once power is restored the buffer is paid down and deposited, with
    * no fluid lost along the way.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 400)
    public void testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(GameTestHelper context) {
        FluidSupplierGameTestBody.testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(context);
    }

    /**
    * No real room: the machine tank is already full to its physical capacity, but the configured
    * target is set higher than that capacity. The supplier must stop ordering rather than
    * perpetually re-requesting fluid the tank can never actually hold.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 200)
    public void testStopsOrderingWhenTankHasNoRealRoom(GameTestHelper context) {
        FluidSupplierGameTestBody.testStopsOrderingWhenTankHasNoRealRoom(context);
    }
}
