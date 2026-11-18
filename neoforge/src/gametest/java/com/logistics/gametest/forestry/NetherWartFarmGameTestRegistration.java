package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link NetherWartFarmGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class NetherWartFarmGameTestRegistration {

    private NetherWartFarmGameTestRegistration() {}

    @GameTest(template = "empty", batch = "netherwartfarm", timeoutTicks = 1200)
    public static void testHarvestsAndReplantsAndHopperPullsHarvest(GameTestHelper context) {
        NetherWartFarmGameTestBody.testHarvestsAndReplantsAndHopperPullsHarvest(context);
    }

    @GameTest(template = "empty", batch = "netherwartfarm", timeoutTicks = 1200)
    public static void testRefillsEmptyPlotFromHopperFedWart(GameTestHelper context) {
        NetherWartFarmGameTestBody.testRefillsEmptyPlotFromHopperFedWart(context);
    }

    @GameTest(template = "empty", batch = "netherwartfarm", timeoutTicks = 1200)
    public static void testFullFarmSpillsAtThePlot(GameTestHelper context) {
        NetherWartFarmGameTestBody.testFullFarmSpillsAtThePlot(context);
    }

    @GameTest(template = "empty", batch = "netherwartfarm", timeoutTicks = 400)
    public static void testDoesNothingWithoutPower(GameTestHelper context) {
        NetherWartFarmGameTestBody.testDoesNothingWithoutPower(context);
    }
}
