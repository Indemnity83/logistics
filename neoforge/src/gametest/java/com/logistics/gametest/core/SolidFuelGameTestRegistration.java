package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link SolidFuelGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class SolidFuelGameTestRegistration {

    private SolidFuelGameTestRegistration() {}

    @GameTest(template = "empty", batch = "solidfuel", timeoutTicks = 10)
    public static void testPeatBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testPeatBurns(context);
    }

    @GameTest(template = "empty", batch = "solidfuel", timeoutTicks = 10)
    public static void testBitumenBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testBitumenBurns(context);
    }

    @GameTest(template = "empty", batch = "solidfuel", timeoutTicks = 10)
    public static void testTarBurns(GameTestHelper context) {
        SolidFuelGameTestBody.testTarBurns(context);
    }
}
