package com.logistics.gametest.automation;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the Refinery GameTests. Test logic lives in
 * {@link RefineryGameTestBody} (shared with NeoForge — see {@code common/src/gametest}); these
 * methods only carry the {@code @GameTest} annotation Fabric's reflection-based test discovery
 * requires.
 */
public class RefineryGameTest {

    @GameTest
    public void testPlacement(GameTestHelper context) {
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
    @GameTest
    public void testInsertTargetsInputExtractTargetsOutput(GameTestHelper context) {
        RefineryGameTestBody.testInsertTargetsInputExtractTargetsOutput(context);
    }

    /**
     * Wiki claim (Recipes): "Liquid Biomass (200 mB) -> Bio Fuel (100 mB)." (Power): "Each recipe
     * costs a total of 5,000 RF."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Recipes">wiki/Refinery.txt § Recipes</a>
     */
    @GameTest(maxTicks = 280)
    public void testDistillsLiquidBiomassIntoBioFuel(GameTestHelper context) {
        RefineryGameTestBody.testDistillsLiquidBiomassIntoBioFuel(context);
    }

    /**
     * Wiki claim (Power): "connect a strong RF source" — each recipe costs a total of 5,000 RF.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Power">wiki/Refinery.txt § Power</a>
     */
    @GameTest(maxTicks = 320)
    public void testDistillsViaRealEngine(GameTestHelper context) {
        RefineryGameTestBody.testDistillsViaRealEngine(context);
    }
}
