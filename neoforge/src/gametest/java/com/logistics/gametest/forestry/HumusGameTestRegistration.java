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
    public static void testOakGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testOakGrowsSmallTree(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testDarkOakGrowsFromOneSapling(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGrowsFromOneSapling(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testSpruceGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testSpruceGrowsSmallTree(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testAzaleaGrowsSmallTree(GameTestHelper context) {
        HumusGameTestBody.testAzaleaGrowsSmallTree(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testMangroveGrowsSmallTreeWithGrownPropagules(GameTestHelper context) {
        HumusGameTestBody.testMangroveGrowsSmallTreeWithGrownPropagules(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testDarkOakGroupGrowsFourTrees(GameTestHelper context) {
        HumusGameTestBody.testDarkOakGroupGrowsFourTrees(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testSpruceGroupGrowsFourTrees(GameTestHelper context) {
        HumusGameTestBody.testSpruceGroupGrowsFourTrees(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testSaplingsOffHumusGrowAsVanilla(GameTestHelper context) {
        HumusGameTestBody.testSaplingsOffHumusGrowAsVanilla(context);
    }

    @GameTest(template = "empty", batch = "humus", timeoutTicks = 60)
    public static void testHumusIsPermanentAndGivesNoBoost(GameTestHelper context) {
        HumusGameTestBody.testHumusIsPermanentAndGivesNoBoost(context);
    }
}
