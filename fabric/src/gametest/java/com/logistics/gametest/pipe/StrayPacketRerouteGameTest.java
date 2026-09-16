package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the stray-packet reroute GameTests. Test logic lives in
 * {@link StrayPacketRerouteGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Run in-game: /test run logistics-gametest.straypacketreroutegametest.&lt;methodname&gt;
 */
public class StrayPacketRerouteGameTest {

    /** A stray stack with no destination is re-homed onto a supplier's standing order. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 220)
    public void testStrayItemReachesAnOrderingSupplier(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayItemReachesAnOrderingSupplier(context);
    }

    /** Control: with nothing ordering iron, the same stray stack is still dropped on the floor. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 160)
    public void testStrayItemWithNoOrderStillDrops(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayItemWithNoOrderStillDrops(context);
    }

    /** A stray fluid packet is re-homed rather than voided — the case that destroys mB today. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 260)
    public void testStrayFluidPacketReachesAnOrderingSupplier(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayFluidPacketReachesAnOrderingSupplier(context);
    }
}
