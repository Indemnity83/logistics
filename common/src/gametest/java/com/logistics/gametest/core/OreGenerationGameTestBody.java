package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Shared ore-generation GameTest bodies, compiled directly into both loaders' {@code gametest}
 * source sets (see {@code common/build.gradle}). Loader-specific glue wires these into each
 * loader's own registration mechanism: Fabric's {@code @GameTest}-annotated
 * {@code OreGenerationGameTest} delegates to these methods, and NeoForge's
 * {@code OreGenerationGameTestRegistration} references them directly as
 * {@code Consumer<GameTestHelper>} method references.
 *
 * <p>Verifies that tin and apatite ore generation is properly configured.
 */
public class OreGenerationGameTestBody {

    /**
     * Test that tin ore (stone variant) placed feature is registered.
     */
    public static void testTinOreStoneFeatureRegistered(GameTestHelper context) {
        // Verify the placed feature is registered in the registry
        ResourceKey<PlacedFeature> featureKey = ResourceKey.create(
            Registries.PLACED_FEATURE,
            LogisticsMod.modId("tin_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.PLACED_FEATURE);
        if (registry.isEmpty()) {
            context.fail("Placed feature registry not available");
            return;
        }

        var feature = registry.get().get(featureKey);
        if (feature.isEmpty()) {
            context.fail("Tin ore stone placed feature not registered");
            return;
        }

        context.succeed();
    }

    /**
     * Test that tin ore (deepslate variant) placed feature is registered.
     */
    public static void testTinOreDeepslateFeatureRegistered(GameTestHelper context) {
        ResourceKey<PlacedFeature> featureKey = ResourceKey.create(
            Registries.PLACED_FEATURE,
            LogisticsMod.modId("tin_ore_deepslate").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.PLACED_FEATURE);
        if (registry.isEmpty()) {
            context.fail("Placed feature registry not available");
            return;
        }

        var feature = registry.get().get(featureKey);
        if (feature.isEmpty()) {
            context.fail("Tin ore deepslate placed feature not registered");
            return;
        }

        context.succeed();
    }

    /**
     * Test that apatite ore placed feature is registered.
     */
    public static void testApatiteOreFeatureRegistered(GameTestHelper context) {
        ResourceKey<PlacedFeature> featureKey = ResourceKey.create(
            Registries.PLACED_FEATURE,
            LogisticsMod.modId("apatite_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.PLACED_FEATURE);
        if (registry.isEmpty()) {
            context.fail("Placed feature registry not available");
            return;
        }

        var feature = registry.get().get(featureKey);
        if (feature.isEmpty()) {
            context.fail("Apatite ore placed feature not registered");
            return;
        }

        context.succeed();
    }

    /**
     * Test that tin ore configured feature is registered.
     */
    public static void testTinOreConfiguredFeatureRegistered(GameTestHelper context) {
        ResourceKey<Feature> featureKey = ResourceKey.create(
            Registries.FEATURE,
            LogisticsMod.modId("tin_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return;
        }

        var feature = registry.get().get(featureKey);
        if (feature.isEmpty()) {
            context.fail("Tin ore configured feature not registered");
            return;
        }

        context.succeed();
    }

    /**
     * Test that apatite ore configured feature is registered.
     */
    public static void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
        ResourceKey<Feature> featureKey = ResourceKey.create(
            Registries.FEATURE,
            LogisticsMod.modId("apatite_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return;
        }

        var feature = registry.get().get(featureKey);
        if (feature.isEmpty()) {
            context.fail("Apatite ore configured feature not registered");
            return;
        }

        context.succeed();
    }

    /**
     * Test that tin ore blocks can be placed and are recognized.
     */
    public static void testTinOreBlocksPlaceable(GameTestHelper context) {
        BlockPos stoneOrePos = new BlockPos(1, 1, 1);
        BlockPos deepslateOrePos = new BlockPos(2, 1, 1);

        // Place tin ore blocks
        context.setBlock(stoneOrePos, LogisticsCore.BLOCK.TIN_ORE);
        context.setBlock(deepslateOrePos, LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE);

        // Verify blocks are placed correctly
        if (!context.getBlockState(stoneOrePos).is(LogisticsCore.BLOCK.TIN_ORE)) {
            context.fail("Tin ore block not placed correctly");
            return;
        }

        if (!context.getBlockState(deepslateOrePos).is(LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE)) {
            context.fail("Deepslate tin ore block not placed correctly");
            return;
        }

        context.succeed();
    }

    /**
     * Test that apatite ore block can be placed and is recognized.
     */
    public static void testApatiteOreBlockPlaceable(GameTestHelper context) {
        BlockPos orePos = new BlockPos(1, 1, 1);

        // Place apatite ore block
        context.setBlock(orePos, LogisticsCore.BLOCK.APATITE_ORE);

        // Verify block is placed correctly
        if (!context.getBlockState(orePos).is(LogisticsCore.BLOCK.APATITE_ORE)) {
            context.fail("Apatite ore block not placed correctly");
            return;
        }

        context.succeed();
    }

    /**
     * Test that tin ore (stone variant) target predicate accepts stone blocks.
     * Verifies the ore generation target configuration is correct.
     */
    public static void testTinOreCanReplaceStone(GameTestHelper context) {
        // Get the configured feature from registry
        ResourceKey<Feature> featureKey = ResourceKey.create(
            Registries.FEATURE,
            LogisticsMod.modId("tin_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return;
        }

        var featureHolder = registry.get().get(featureKey);
        if (featureHolder.isEmpty()) {
            context.fail("Tin ore stone configured feature not found");
            return;
        }

        // An ore feature carries its own target list in 26.3 — there is no separate configuration.
        Feature feature = featureHolder.get().value();
        if (!(feature instanceof AbstractOreFeature oreFeature)) {
            context.fail("Tin ore stone feature is not an OreConfiguration");
            return;
        }

        // Test that the target predicate accepts stone blocks
        boolean foundStoneTarget = false;
        for (var target : oreFeature.targetStates()) {
            if (target.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, context.getLevel().getRandom())) {
                foundStoneTarget = true;
                // Verify it places tin ore
                if (!target.state().is(LogisticsCore.BLOCK.TIN_ORE)) {
                    context.fail("Stone target does not place tin ore, places: " + target.state().getBlock());
                    return;
                }
                break;
            }
        }

        if (!foundStoneTarget) {
            context.fail("Tin ore configuration does not target stone blocks");
            return;
        }

        context.succeed();
    }

    /**
     * Test that deepslate tin ore target predicate accepts deepslate blocks.
     * Verifies the deepslate variant ore generation target configuration is correct.
     */
    public static void testTinOreCanReplaceDeepslate(GameTestHelper context) {
        // Get the configured feature from registry
        ResourceKey<Feature> featureKey = ResourceKey.create(
            Registries.FEATURE,
            LogisticsMod.modId("tin_ore_deepslate").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return;
        }

        var featureHolder = registry.get().get(featureKey);
        if (featureHolder.isEmpty()) {
            context.fail("Tin ore deepslate configured feature not found");
            return;
        }

        // An ore feature carries its own target list in 26.3 — there is no separate configuration.
        Feature feature = featureHolder.get().value();
        if (!(feature instanceof AbstractOreFeature oreFeature)) {
            context.fail("Tin ore deepslate feature is not an OreConfiguration");
            return;
        }

        // Test that the target predicate accepts deepslate blocks
        boolean foundDeepslateTarget = false;
        for (var target : oreFeature.targetStates()) {
            if (target.target().test(Blocks.DEEPSLATE.defaultBlockState(), BlockPos.ZERO, context.getLevel().getRandom())) {
                foundDeepslateTarget = true;
                // Verify it places deepslate tin ore
                if (!target.state().is(LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE)) {
                    context.fail("Deepslate target does not place deepslate tin ore, places: " + target.state().getBlock());
                    return;
                }
                break;
            }
        }

        if (!foundDeepslateTarget) {
            context.fail("Deepslate tin ore configuration does not target deepslate blocks");
            return;
        }

        context.succeed();
    }

    /**
     * Test that apatite ore target predicate accepts stone blocks.
     * Verifies the ore generation target configuration is correct.
     */
    public static void testApatiteOreCanReplaceStone(GameTestHelper context) {
        // Get the configured feature from registry
        ResourceKey<Feature> featureKey = ResourceKey.create(
            Registries.FEATURE,
            LogisticsMod.modId("apatite_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return;
        }

        var featureHolder = registry.get().get(featureKey);
        if (featureHolder.isEmpty()) {
            context.fail("Apatite ore stone configured feature not found");
            return;
        }

        // An ore feature carries its own target list in 26.3 — there is no separate configuration.
        Feature feature = featureHolder.get().value();
        if (!(feature instanceof AbstractOreFeature oreFeature)) {
            context.fail("Apatite ore stone feature is not an OreConfiguration");
            return;
        }

        // Test that the target predicate accepts stone blocks
        boolean foundStoneTarget = false;
        for (var target : oreFeature.targetStates()) {
            if (target.target().test(Blocks.STONE.defaultBlockState(), BlockPos.ZERO, context.getLevel().getRandom())) {
                foundStoneTarget = true;
                // Verify it places apatite ore
                if (!target.state().is(LogisticsCore.BLOCK.APATITE_ORE)) {
                    context.fail("Stone target does not place apatite ore, places: " + target.state().getBlock());
                    return;
                }
                break;
            }
        }

        if (!foundStoneTarget) {
            context.fail("Apatite ore configuration does not target stone blocks");
            return;
        }

        context.succeed();
    }
}
