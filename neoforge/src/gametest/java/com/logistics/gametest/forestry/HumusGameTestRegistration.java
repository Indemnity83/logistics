package com.logistics.gametest.forestry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link HumusGameTestBody}'s methods into NeoForge's GameTest discovery — see
 * {@link com.logistics.gametest.RecipeLoadingGameTestRegistration} for the legacy registration
 * model 1.21.1 uses.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class HumusGameTestRegistration {

    private HumusGameTestRegistration() {}

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testSaplingSurvivesOnHumus(GameTestHelper context) {
        HumusGameTestBody.testSaplingSurvivesOnHumus(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testHumusBoostGrowsTreeAndTurnsToSand(GameTestHelper context) {
        HumusGameTestBody.testHumusBoostGrowsTreeAndTurnsToSand(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testHumusUnderLogTurnsToSand(GameTestHelper context) {
        HumusGameTestBody.testHumusUnderLogTurnsToSand(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testHumusWithoutSaplingNeverChanges(GameTestHelper context) {
        HumusGameTestBody.testHumusWithoutSaplingNeverChanges(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testNoGrowthInDarkness(GameTestHelper context) {
        HumusGameTestBody.testNoGrowthInDarkness(context);
    }
}
