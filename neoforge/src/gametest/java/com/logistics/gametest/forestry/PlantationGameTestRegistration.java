package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link PlantationGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class PlantationGameTestRegistration {

    private PlantationGameTestRegistration() {}

    @GameTest(template = "empty", batch = "plantation", timeoutTicks = 600)
    public static void testHarvestsStalksAndKeepsEveryBase(GameTestHelper context) {
        PlantationGameTestBody.testHarvestsStalksAndKeepsEveryBase(context);
    }

    @GameTest(template = "empty", batch = "plantation", timeoutTicks = 400)
    public static void testFullFarmSpillsAtTheBase(GameTestHelper context) {
        PlantationGameTestBody.testFullFarmSpillsAtTheBase(context);
    }

    @GameTest(template = "empty", batch = "plantation", timeoutTicks = 400)
    public static void testReplantsAMissingBaseFromTheInventory(GameTestHelper context) {
        PlantationGameTestBody.testReplantsAMissingBaseFromTheInventory(context);
    }
}
