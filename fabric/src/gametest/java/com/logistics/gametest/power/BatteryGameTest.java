package com.logistics.gametest.power;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the battery GameTests. Test logic lives in
 * {@link BatteryGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 *
 * <p>Tests the charge-state rendering signal. A battery no longer powers the logistics pipe
 * network directly — that bridge is now the Power Junction (see
 * {@code com.logistics.gametest.pipe.PowerJunctionGameTest}).
 */
public class BatteryGameTest {

    /** A placed battery has its block entity. */
    @GameTest
    public void testBatteryPlacement(GameTestHelper context) {
        BatteryGameTestBody.testBatteryPlacement(context);
    }

    /** The CHARGE block state property tracks stored energy (drives the multipart fill bar). */
    @GameTest(maxTicks = 30)
    public void testBatteryChargeStateTracksEnergy(GameTestHelper context) {
        BatteryGameTestBody.testBatteryChargeStateTracksEnergy(context);
    }

    /** A network with no power source cannot supply energy (the hard power gate). */
    @GameTest(maxTicks = 40)
    public void testNetworkWithoutBatteryIsUnpowered(GameTestHelper context) {
        BatteryGameTestBody.testNetworkWithoutBatteryIsUnpowered(context);
    }

    /** A charged battery alone no longer powers the network — only a Power Junction bridges RF in. */
    @GameTest(maxTicks = 40)
    public void testChargedBatteryDoesNotPowerNetwork(GameTestHelper context) {
        BatteryGameTestBody.testChargedBatteryDoesNotPowerNetwork(context);
    }
}
