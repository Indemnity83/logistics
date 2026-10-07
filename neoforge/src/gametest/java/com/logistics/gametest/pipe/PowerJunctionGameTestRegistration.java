package com.logistics.gametest.pipe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link PowerJunctionGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class PowerJunctionGameTestRegistration {

    private PowerJunctionGameTestRegistration() {}

    /** A filled junction adjacent to a pipe powers that pipe's network and is drawn down. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testJunctionPowersNetwork(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionPowersNetwork(context);
    }

    /** An unfilled junction supplies nothing (the hard power gate). */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testEmptyJunctionDoesNotPowerNetwork(GameTestHelper context) {
        PowerJunctionGameTestBody.testEmptyJunctionDoesNotPowerNetwork(context);
    }

    /** An adjacent pipe forms a POWER connection (rendered arm) toward the junction, not a route. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testPipeFormsPowerConnectionToJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testPipeFormsPowerConnectionToJunction(context);
    }

    /** A filled junction makes a logistics pipe's junction-facing and pipe-facing arms "powered". */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testLogisticsArmsPoweredWhenJunctionCharged(GameTestHelper context) {
        PowerJunctionGameTestBody.testLogisticsArmsPoweredWhenJunctionCharged(context);
    }

    /** A machine that speaks the PIPE connection only for item I/O (the quarry) is not a power link. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testQuarryArmIsNotTreatedAsPower(GameTestHelper context) {
        PowerJunctionGameTestBody.testQuarryArmIsNotTreatedAsPower(context);
    }

    /** The logistics network may draw its whole junction buffer in one tick. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 80)
    public static void testNetworkDrawsPastTheOldOutputCap(GameTestHelper context) {
        PowerJunctionGameTestBody.testNetworkDrawsPastTheOldOutputCap(context);
    }

    /** A cable may fill a junction but never empty one. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 110)
    public static void testCableCannotDrainTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableCannotDrainTheJunction(context);
    }

    /** A junction must power the network it now belongs to as soon as a split creates it. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 80)
    public static void testJunctionPowersNewNetworkAfterSplit(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionPowersNewNetworkAfterSplit(context);
    }

    /** An engine fills a junction through a cable, not only when placed against it. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 60)
    public static void testCableFillsTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableFillsTheJunction(context);
    }

    /** Control: the same engine and junction with no cable between them. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 60)
    public static void testAdjacentEngineFillsTheJunction(GameTestHelper context) {
        PowerJunctionGameTestBody.testAdjacentEngineFillsTheJunction(context);
    }

    /** A fuelled Magmatic Engine fills a junction through a cable. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 180)
    public static void testMagmaticEngineFillsJunctionThroughCable(GameTestHelper context) {
        PowerJunctionGameTestBody.testMagmaticEngineFillsJunctionThroughCable(context);
    }

    /** A cable directly above a junction fills it — no face is special. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 60)
    public static void testCableFillsTheJunctionFromAbove(GameTestHelper context) {
        PowerJunctionGameTestBody.testCableFillsTheJunctionFromAbove(context);
    }

    /** Every face of a junction exposes the same insert-capable storage. */
    @GameTest(template = "empty", batch = "powerjunction", timeoutTicks = 40)
    public static void testJunctionAcceptsEnergyOnEveryFace(GameTestHelper context) {
        PowerJunctionGameTestBody.testJunctionAcceptsEnergyOnEveryFace(context);
    }
}
