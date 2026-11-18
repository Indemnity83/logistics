package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link OrchardGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class OrchardGameTestRegistration {

    private OrchardGameTestRegistration() {}

    @GameTest(template = "empty", batch = "orchard", timeoutTicks = 1200)
    public static void testPicksApplesWithoutBreakingTheTree(GameTestHelper context) {
        OrchardGameTestBody.testPicksApplesWithoutBreakingTheTree(context);
    }

    @GameTest(template = "empty", batch = "orchard", timeoutTicks = 400)
    public static void testPicksNothingWithoutPower(GameTestHelper context) {
        OrchardGameTestBody.testPicksNothingWithoutPower(context);
    }
}
