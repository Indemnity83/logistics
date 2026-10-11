package com.logistics.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link HarnessGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link RecipeLoadingGameTestRegistration} for the legacy registration model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class HarnessGameTestRegistration {

    private HarnessGameTestRegistration() {}

    @GameTest(template = "empty", batch = "harness", timeoutTicks = 20)
    public static void testStructureCoversTheSharedFootprint(GameTestHelper context) {
        HarnessGameTestBody.testStructureCoversTheSharedFootprint(context);
    }
}
