package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link MaceratorGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class MaceratorGameTestRegistration {

    private MaceratorGameTestRegistration() {}

    @GameTest(template = "empty", batch = "macerator")
    public static void testPlacementCreatesBlockEntity(GameTestHelper context) {
        MaceratorGameTestBody.testPlacementCreatesBlockEntity(context);
    }

    /**
    * Wiki claim (Usage): "Input is accepted from the top and sides; output is drawn from the
    * bottom."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "macerator")
    public static void testSidedAccess(GameTestHelper context) {
        MaceratorGameTestBody.testSidedAccess(context);
    }

    @GameTest(template = "empty", batch = "macerator")
    public static void testCapabilitiesExposed(GameTestHelper context) {
        MaceratorGameTestBody.testCapabilitiesExposed(context);
    }

    /**
    * Wiki claim (Usage): "...most ores take 2,000 RF (10 seconds)." (Recipes § Ores → Dust):
    * "Iron Ore -> Iron Dust,2".
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "macerator", timeoutTicks = 240)
    public static void testMaceratesOreWithEnergy(GameTestHelper context) {
        MaceratorGameTestBody.testMaceratesOreWithEnergy(context);
    }

    /**
    * Wiki claim (Usage/Power): "Input is accepted from the top and sides... connect a Stirling
    * Engine or any RF source."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Macerator#Usage">wiki/Macerator.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "macerator", timeoutTicks = 260)
    public static void testMaceratesViaRealEngineAndHoppers(GameTestHelper context) {
        MaceratorGameTestBody.testMaceratesViaRealEngineAndHoppers(context);
    }
}
