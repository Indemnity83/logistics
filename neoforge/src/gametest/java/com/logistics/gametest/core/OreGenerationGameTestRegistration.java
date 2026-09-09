package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Wires {@link OreGenerationGameTestBody}'s methods into NeoForge's GameTest discovery.
 *
 * <p>MC 1.21.1 predates the data-driven {@code TEST_FUNCTION} registry, so registration
 * here is the legacy reflection model: {@link GameTestHolder} makes NeoForge scan this
 * type, and {@link PrefixGameTestTemplate}(false) keeps the class name out of the
 * template id so every test can share {@code logistics_gametest:empty}.
 */
@GameTestHolder("logistics_gametest")
@PrefixGameTestTemplate(false)
public final class OreGenerationGameTestRegistration {

    private OreGenerationGameTestRegistration() {}

    /**
    * Test that tin ore (stone variant) placed feature is registered.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreStoneFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreStoneFeatureRegistered(context);
    }

    /**
    * Test that tin ore (deepslate variant) placed feature is registered.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreDeepslateFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreDeepslateFeatureRegistered(context);
    }

    /**
    * Test that apatite ore placed feature is registered.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testApatiteOreFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreFeatureRegistered(context);
    }

    /**
    * Test that tin ore configured feature is registered.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreConfiguredFeatureRegistered(context);
    }

    /**
    * Test that apatite ore configured feature is registered.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreConfiguredFeatureRegistered(context);
    }

    /**
    * Test that tin ore blocks can be placed and are recognized.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreBlocksPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreBlocksPlaceable(context);
    }

    /**
    * Test that apatite ore block can be placed and is recognized.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testApatiteOreBlockPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreBlockPlaceable(context);
    }

    /**
    * Test that the real tin ore feature turns stone into tin ore when it generates.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreStoneFeatureGeneratesTinOre(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreStoneFeatureGeneratesTinOre(context);
    }


    /**
    * Test that the real deepslate tin ore feature turns deepslate into deepslate tin ore.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreDeepslateFeatureGeneratesDeepslateTinOre(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreDeepslateFeatureGeneratesDeepslateTinOre(context);
    }


    /**
    * Test that tin ore (stone variant) declares stone as its replace target.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreTargetsStone(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreTargetsStone(context);
    }


    /**
    * Test that deepslate tin ore declares deepslate as its replace target.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreTargetsDeepslate(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreTargetsDeepslate(context);
    }


    /**
    * Test that apatite ore declares stone as its replace target.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testApatiteOreTargetsStone(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreTargetsStone(context);
    }

}
