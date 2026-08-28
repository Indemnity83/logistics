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
    * Test that tin ore (stone variant) target predicate accepts stone blocks.
    * Verifies the ore generation target configuration is correct.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreCanReplaceStone(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreCanReplaceStone(context);
    }

    /**
    * Test that deepslate tin ore target predicate accepts deepslate blocks.
    * Verifies the deepslate variant ore generation target configuration is correct.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testTinOreCanReplaceDeepslate(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreCanReplaceDeepslate(context);
    }

    /**
    * Test that apatite ore target predicate accepts stone blocks.
    * Verifies the ore generation target configuration is correct.
    */
    @GameTest(template = "empty", batch = "oregeneration")
    public static void testApatiteOreCanReplaceStone(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreCanReplaceStone(context);
    }
}
