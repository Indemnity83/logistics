package com.logistics.gametest.core;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric entrypoint wiring for the ore-generation GameTests. Test logic lives in
 * {@link OreGenerationGameTestBody} (shared with NeoForge — see {@code common/src/gametest});
 * these methods only carry the {@code @GameTest} annotation Fabric's reflection-based test
 * discovery requires.
 */
public class OreGenerationGameTest {

    /**
     * Test that tin ore (stone variant) placed feature is registered.
     */
    @GameTest
    public void testTinOreStoneFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreStoneFeatureRegistered(context);
    }

    /**
     * Test that tin ore (deepslate variant) placed feature is registered.
     */
    @GameTest
    public void testTinOreDeepslateFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreDeepslateFeatureRegistered(context);
    }

    /**
     * Test that apatite ore placed feature is registered.
     */
    @GameTest
    public void testApatiteOreFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreFeatureRegistered(context);
    }

    /**
     * Test that tin ore configured feature is registered.
     */
    @GameTest
    public void testTinOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreConfiguredFeatureRegistered(context);
    }

    /**
     * Test that apatite ore configured feature is registered.
     */
    @GameTest
    public void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreConfiguredFeatureRegistered(context);
    }

    /**
     * Test that tin ore blocks can be placed and are recognized.
     */
    @GameTest
    public void testTinOreBlocksPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreBlocksPlaceable(context);
    }

    /**
     * Test that apatite ore block can be placed and is recognized.
     */
    @GameTest
    public void testApatiteOreBlockPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreBlockPlaceable(context);
    }

    /**
     * Test that tin ore (stone variant) target predicate accepts stone blocks.
     * Verifies the ore generation target configuration is correct.
     */
    @GameTest
    public void testTinOreCanReplaceStone(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreCanReplaceStone(context);
    }

    /**
     * Test that deepslate tin ore target predicate accepts deepslate blocks.
     * Verifies the deepslate variant ore generation target configuration is correct.
     */
    @GameTest
    public void testTinOreCanReplaceDeepslate(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreCanReplaceDeepslate(context);
    }

    /**
     * Test that apatite ore target predicate accepts stone blocks.
     * Verifies the ore generation target configuration is correct.
     */
    @GameTest
    public void testApatiteOreCanReplaceStone(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreCanReplaceStone(context);
    }
}
