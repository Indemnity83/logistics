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
}
