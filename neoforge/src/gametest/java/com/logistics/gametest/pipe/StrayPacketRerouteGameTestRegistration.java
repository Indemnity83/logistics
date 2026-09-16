package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link StrayPacketRerouteGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration here is the
 * legacy reflection model: {@link GameTestHolder} makes NeoForge scan this type, and
 * {@link PrefixGameTestTemplate}(false) keeps the class name out of the template id.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class StrayPacketRerouteGameTestRegistration {

    private StrayPacketRerouteGameTestRegistration() {}

    /**
    * A stray stack with no destination is re-homed onto a supplier's standing order and physically
    * delivered. The supplier is the only thing in the layout that wants iron, it registers no sink,
    * and there is no provider — so the order book is the only path from the pipe to the chest.
    */
    @GameTest(template = "empty", batch = "straypacket", timeoutTicks = 240)
    public static void testStrayItemReachesAnOrderingSupplier(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayItemReachesAnOrderingSupplier(context);
    }

    /**
    * Control: with nothing ordering iron, the same stray stack is still dropped on the floor. Without
    * this, the test above would also pass if some other route quietly delivered the stack.
    */
    @GameTest(template = "empty", batch = "straypacket", timeoutTicks = 180)
    public static void testStrayItemWithNoOrderStillDrops(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayItemWithNoOrderStillDrops(context);
    }

    /**
    * A stray fluid packet is re-homed the same way. This is the case that actually destroys resources
    * today: an undeliverable packet is voided rather than dropped, so the mB it carries is gone. No
    * fluid provider exists here, so the only water that can reach the refinery is the stray packet.
    */
    @GameTest(template = "empty", batch = "straypacket", timeoutTicks = 280)
    public static void testStrayFluidPacketReachesAnOrderingSupplier(GameTestHelper context) {
        StrayPacketRerouteGameTestBody.testStrayFluidPacketReachesAnOrderingSupplier(context);
    }
}
