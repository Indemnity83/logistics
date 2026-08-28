package com.logistics.gametest.automation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link RefineryGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class RefineryGameTestRegistration {

    private RefineryGameTestRegistration() {}

    @GameTest(template = "empty", batch = "refinery")
    public static void testPlacement(GameTestHelper context) {
        RefineryGameTestBody.testPlacement(context);
    }

    /**
    * Wiki claim (Usage): "Pipe a fluid into the input tank; the Refinery processes it over time and
    * deposits the product into its output tank, from which a Fluid Extractor Pipe or Pump can draw
    * it." Insertion always targets the input tank and extraction always targets the output tank,
    * on every side — there's no per-face routing to get wrong.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Refinery#Usage">wiki/Refinery.txt § Usage</a>
    */
    @GameTest(template = "empty", batch = "refinery")
    public static void testInsertTargetsInputExtractTargetsOutput(GameTestHelper context) {
        RefineryGameTestBody.testInsertTargetsInputExtractTargetsOutput(context);
    }

    /**
    * Wiki claim (Recipes): "Liquid Biomass (200 mB) -> Bio Fuel (100 mB)." (Power): "Each recipe
    * costs a total of 5,000 RF."
    *
    * @see <a href="https://logistics.fandom.com/wiki/Refinery#Recipes">wiki/Refinery.txt § Recipes</a>
    */
    @GameTest(template = "empty", batch = "refinery", timeoutTicks = 280)
    public static void testDistillsLiquidBiomassIntoBioFuel(GameTestHelper context) {
        RefineryGameTestBody.testDistillsLiquidBiomassIntoBioFuel(context);
    }

    /**
    * Wiki claim (Power): "connect a strong RF source" — each recipe costs a total of 5,000 RF.
    *
    * @see <a href="https://logistics.fandom.com/wiki/Refinery#Power">wiki/Refinery.txt § Power</a>
    */
    @GameTest(template = "empty", batch = "refinery", timeoutTicks = 320)
    public static void testDistillsViaRealEngine(GameTestHelper context) {
        RefineryGameTestBody.testDistillsViaRealEngine(context);
    }
}
