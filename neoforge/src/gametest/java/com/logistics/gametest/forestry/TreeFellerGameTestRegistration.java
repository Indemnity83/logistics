package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link TreeFellerGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class TreeFellerGameTestRegistration {

    private TreeFellerGameTestRegistration() {}

    @GameTest(template = "empty", batch = "treefeller", timeoutTicks = 100)
    public static void testLevelTreeViewClassifiesAndPlansATree(GameTestHelper context) {
        TreeFellerGameTestBody.testLevelTreeViewClassifiesAndPlansATree(context);
    }
}
