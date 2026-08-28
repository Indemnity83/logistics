package com.logistics.gametest.power;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link FuelEngineGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class FuelEngineGameTestRegistration {

    private FuelEngineGameTestRegistration() {}

    /** A powered engine with fuel + water generates RF and warms up. */
    @GameTest(template = "empty", batch = "fuelengine", timeoutTicks = 40)
    public static void testFuelEngineGeneratesFromFuel(GameTestHelper context) {
        FuelEngineGameTestBody.testFuelEngineGeneratesFromFuel(context);
    }

    /** Without coolant the engine overheats; a wrench-style reset clears the shutdown and preserves tank fuel. */
    @GameTest(template = "empty", batch = "fuelengine", timeoutTicks = 140)
    public static void testFuelEngineOverheatsWithoutCoolantThenResets(GameTestHelper context) {
        FuelEngineGameTestBody.testFuelEngineOverheatsWithoutCoolantThenResets(context);
    }

    /** The combined fluid view routes inserts by type: water to the coolant tank, fuel to the fuel tank. */
    @GameTest(template = "empty", batch = "fuelengine")
    public static void testFluidInsertRoutesByType(GameTestHelper context) {
        FuelEngineGameTestBody.testFluidInsertRoutesByType(context);
    }
}
