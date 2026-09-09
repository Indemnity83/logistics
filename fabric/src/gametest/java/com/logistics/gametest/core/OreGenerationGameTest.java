package com.logistics.gametest.core;

import net.minecraft.gametest.framework.GameTest;
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
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreStoneFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreStoneFeatureRegistered(context);
    }

    /**
    * Test that tin ore (deepslate variant) placed feature is registered.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreDeepslateFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreDeepslateFeatureRegistered(context);
    }

    /**
    * Test that apatite ore placed feature is registered.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testApatiteOreFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreFeatureRegistered(context);
    }

    /**
    * Test that tin ore configured feature is registered.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreConfiguredFeatureRegistered(context);
    }

    /**
    * Test that apatite ore configured feature is registered.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreConfiguredFeatureRegistered(context);
    }

    /**
    * Test that tin ore blocks can be placed and are recognized.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreBlocksPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreBlocksPlaceable(context);
    }

    /**
    * Test that apatite ore block can be placed and is recognized.
    */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testApatiteOreBlockPlaceable(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreBlockPlaceable(context);
    }

    /**
     * Test that the real tin ore feature turns stone into tin ore when it generates.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreStoneFeatureGeneratesTinOre(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreStoneFeatureGeneratesTinOre(context);
    }

    /**
     * Test that the real deepslate tin ore feature turns deepslate into deepslate tin ore.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreDeepslateFeatureGeneratesDeepslateTinOre(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreDeepslateFeatureGeneratesDeepslateTinOre(context);
    }

    /**
     * Test that tin ore (stone variant) declares stone as its replace target.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreTargetsStone(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreTargetsStone(context);
    }

    /**
     * Test that deepslate tin ore declares deepslate as its replace target.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testTinOreTargetsDeepslate(GameTestHelper context) {
        OreGenerationGameTestBody.testTinOreTargetsDeepslate(context);
    }

    /**
     * Test that apatite ore declares stone as its replace target.
     */
    @GameTest(template = "fabric-gametest-api-v1:empty")
    public void testApatiteOreTargetsStone(GameTestHelper context) {
        OreGenerationGameTestBody.testApatiteOreTargetsStone(context);
    }
}
