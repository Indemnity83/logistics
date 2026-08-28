package com.logistics.gametest.pipe;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
    @GameTest(maxTicks = 260)
    public void testDryRestockFillsToTarget(GameTestHelper context) {
        FluidSupplierGameTestBody.testDryRestockFillsToTarget(context);
    }

    /**
     * A non-round deficit (e.g. 137 mB) is ordered and fulfilled exactly — proving "no minimum packet
     * size" end to end at the consumer, not just rounded down to the nearest packet multiple.
     */
    @GameTest(maxTicks = 260)
    public void testExactNonRoundDeficitIsOrderedAndFilled(GameTestHelper context) {
        FluidSupplierGameTestBody.testExactNonRoundDeficitIsOrderedAndFilled(context);
    }

    // Note: "exactly-once acknowledgement across several physical packets sharing one delivery id" is
    // deliberately NOT tested end-to-end against the refinery here — with the default 5000 mB packet
    // max exceeding the refinery's 4000 mB input tank, a single order into this machine can never
    // require more than one physical packet, so "two full + one tail" isn't reachable through this
    // destination. That scenario is covered instead by `FluidProviderGameTest
    // #testMixedFullPlusTailDispatchChargesPerPhysicalPacket` (a plain chest destination, unconstrained
    // by machine tank capacity) and by `FluidOrderBookTest`'s multi-packet/multi-tick unit coverage.

    /**
     * No over-reserve: across many ticks the supplier never has more fluid held plus on order than its
     * buffer can hold (held + pending ≤ capacity). Target exceeds a single batch so ordering stays busy.
     */
    @GameTest(maxTicks = 200)
    public void testNeverOrdersMoreThanBufferCanHold(GameTestHelper context) {
        FluidSupplierGameTestBody.testNeverOrdersMoreThanBufferCanHold(context);
    }

    /**
     * Insufficient energy: the network holds only enough energy for the provider to mint packets, none
     * left for the supplier to "pay" for them. Delivered packets are accepted into the buffer but no
     * fluid reaches the machine tank. Once power is restored the buffer is paid down and deposited, with
     * no fluid lost along the way.
     */
    @GameTest(maxTicks = 400)
    public void testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(GameTestHelper context) {
        FluidSupplierGameTestBody.testInsufficientEnergyHoldsFluidThenDepositsWhenPowered(context);
    }

    /**
     * No real room: the machine tank is already full to its physical capacity, but the configured
     * target is set higher than that capacity. The supplier must stop ordering rather than
     * perpetually re-requesting fluid the tank can never actually hold.
     */
    @GameTest(maxTicks = 200)
    public void testStopsOrderingWhenTankHasNoRealRoom(GameTestHelper context) {
        FluidSupplierGameTestBody.testStopsOrderingWhenTankHasNoRealRoom(context);
    }

    // Note: "changing fluid_packet_max_mb mid-flight doesn't corrupt accounting" is intentionally NOT
    // covered by a gametest here. Gametest batches run many tests concurrently against the same JVM's
    // static config state (confirmed via debug logging: a config mutation in one test measurably
    // corrupted unrelated tests' packet chunking mid-run) — there is no safe way to mutate a global
    // config value from within a single gametest without risking cross-test interference. The invariant
    // itself is instead proven structurally: `PipeDataComponentsTest` verifies every `FluidPacket`
    // always carries its own real, positive `amountMb`, and `FluidSupplierModule.onTransferToStorage`
    // (see source) accepts any packet matching the filter fluid without comparing against the *current*
    // config value at all — acceptance and bookkeeping are architecturally independent of live config.
}
