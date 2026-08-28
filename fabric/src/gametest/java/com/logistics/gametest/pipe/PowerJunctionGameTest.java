package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the power junction GameTests. Test logic lives in
 * {@link PowerJunctionGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class PowerJunctionGameTest {

    /** A filled junction adjacent to a pipe powers that pipe's network and is drawn down. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testJunctionPowersNetwork(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionPowersNetwork(context);
    }

    /** An unfilled junction supplies nothing (the hard power gate). */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testEmptyJunctionDoesNotPowerNetwork(GameTestHelper context) {
        PowerJunctionGameTestBody.testEmptyJunctionDoesNotPowerNetwork(context);
    }

    /** An adjacent pipe forms a POWER connection (rendered arm) toward the junction, not a route. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testPipeFormsPowerConnectionToJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testPipeFormsPowerConnectionToJunction(context);
    }

    /** A filled junction makes a logistics pipe's junction-facing and pipe-facing arms "powered". */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testLogisticsArmsPoweredWhenJunctionCharged(GameTestHelper context) {
        PowerJunctionGameTestBody.testLogisticsArmsPoweredWhenJunctionCharged(context);
    }

    /** A machine that speaks the PIPE connection only for item I/O (the quarry) is not a power link. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testQuarryArmIsNotTreatedAsPower(GameTestHelper context) {
        PowerJunctionGameTestBody.testQuarryArmIsNotTreatedAsPower(context);
    }
}
