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

    /** A junction must power the network it lands on straight after a split, not a second later. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 80)
    public void testJunctionPowersNewNetworkAfterSplit(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionPowersNewNetworkAfterSplit(context);
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

    /** A cable may fill a junction but never empty one. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 110)
    public void testCableCannotDrainTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableCannotDrainTheJunction(context);
    }

    /** The logistics network may draw its whole junction buffer in one tick. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testNetworkDrawsPastTheOldOutputCap(GameTestHelper context) {
        PowerJunctionGameTestBody.testNetworkDrawsPastTheOldOutputCap(context);
    }

    /** An engine fills a junction through a cable, not only when placed against it. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testCableFillsTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableFillsTheJunction(context);
    }

    /** Control: the same engine and junction with no cable between them. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testAdjacentEngineFillsTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testAdjacentEngineFillsTheJunction(context);
    }

    /** A fuelled Magmatic Engine fills a junction through a cable (the reported scenario). */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 180)
    public void testMagmaticEngineFillsJunctionThroughCable(GameTestHelper context) {
        PowerJunctionGameTestBody.testMagmaticEngineFillsJunctionThroughCable(context);
    }

    /** A cable directly above a junction fills it — no face is special. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 60)
    public void testCableFillsTheJunctionFromAbove(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableFillsTheJunctionFromAbove(context);
    }

    /** Every face of a junction exposes the same insert-capable storage. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void testJunctionAcceptsEnergyOnEveryFace(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionAcceptsEnergyOnEveryFace(context);
    }
}
