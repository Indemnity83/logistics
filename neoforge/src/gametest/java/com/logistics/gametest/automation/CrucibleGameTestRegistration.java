package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link CrucibleGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class CrucibleGameTestRegistration {

    private CrucibleGameTestRegistration() {}

    @GameTest(template = "empty", batch = "crucible")
    public static void testPlacement(GameTestHelper context) {
        CrucibleGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
    * Pipes can drain the tank but never fill it directly — only the melting recipe does.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "crucible")
    public static void testTankIsOutputOnly(GameTestHelper context) {
        CrucibleGameTestBody.testTankIsOutputOnly(context);
    }

    /**
    * Wiki claim (Recipes § Lava & water): "Ice -> Water, Amount=1000."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Lava_.26_water">wiki/Crucible.txt § Lava & water</a>
    */
    @GameTest(template = "empty", batch = "crucible", timeoutTicks = 60)
    public static void testMeltsIceIntoWater(GameTestHelper context) {
        CrucibleGameTestBody.testMeltsIceIntoWater(context);
    }

    /**
    * Wiki claim (Usage): "...deposits the resulting fluid into its 10,000 mB output-only tank."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Crucible#Usage">wiki/Crucible.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "crucible", timeoutTicks = 100)
    public static void testMeltsViaRealEngineAndHopper(GameTestHelper context) {
        CrucibleGameTestBody.testMeltsViaRealEngineAndHopper(context);
    }
}
