package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link BatteryGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class BatteryGameTestRegistration {

    private BatteryGameTestRegistration() {}

    /** A placed battery has its block entity. */
    @GameTest(template = "empty", batch = "battery")
    public static void testBatteryPlacement(GameTestHelper context) {
        BatteryGameTestBody.testBatteryPlacement(context);
    }

    /** The CHARGE block state property tracks stored energy (drives the multipart fill bar). */
    @GameTest(template = "empty", batch = "battery", timeoutTicks = 30)
    public static void testBatteryChargeStateTracksEnergy(GameTestHelper context) {
        BatteryGameTestBody.testBatteryChargeStateTracksEnergy(context);
    }

    /** A network with no power source cannot supply energy (the hard power gate). */
    @GameTest(template = "empty", batch = "battery", timeoutTicks = 40)
    public static void testNetworkWithoutBatteryIsUnpowered(GameTestHelper context) {
        BatteryGameTestBody.testNetworkWithoutBatteryIsUnpowered(context);
    }

    /** A charged battery alone no longer powers the network — only a Power Junction bridges RF in. */
    @GameTest(template = "empty", batch = "battery", timeoutTicks = 40)
    public static void testChargedBatteryDoesNotPowerNetwork(GameTestHelper context) {
        BatteryGameTestBody.testChargedBatteryDoesNotPowerNetwork(context);
    }
}
