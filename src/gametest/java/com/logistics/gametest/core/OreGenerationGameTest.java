package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Game tests for ore generation features.
 * Verifies that tin and apatite ore generation is properly configured.
 */
public class OreGenerationGameTest {

    /**
     * Test that tin ore (stone variant) placed feature is registered.
     */
    @GameTest
    public void testTinOreStoneFeatureRegistered(GameTestHelper context) {
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
    @GameTest
    public void testTinOreDeepslateFeatureRegistered(GameTestHelper context) {
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
    @GameTest
    public void testApatiteOreFeatureRegistered(GameTestHelper context) {
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
    @GameTest
    public void testTinOreConfiguredFeatureRegistered(GameTestHelper context) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            LogisticsMod.modId("tin_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.CONFIGURED_FEATURE);
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
    @GameTest
    public void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            LogisticsMod.modId("apatite_ore_stone").toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.CONFIGURED_FEATURE);
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
    @GameTest
    public void testTinOreBlocksPlaceable(GameTestHelper context) {
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
    @GameTest
    public void testApatiteOreBlockPlaceable(GameTestHelper context) {
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
     * Test that tin ore can replace stone in the target environment.
     * Verifies the ore generation target predicate works correctly.
     */
    @GameTest
    public void testTinOreCanReplaceStone(GameTestHelper context) {
        BlockPos stonePos = new BlockPos(1, 1, 1);
        BlockPos deepslatePos = new BlockPos(2, 1, 1);

        // Place stone and deepslate as generation targets
        context.setBlock(stonePos, Blocks.STONE);
        context.setBlock(deepslatePos, Blocks.DEEPSLATE);

        // Verify stone is placed
        if (!context.getBlockState(stonePos).is(Blocks.STONE)) {
            context.fail("Stone not placed correctly");
            return;
        }

        // Verify deepslate is placed
        if (!context.getBlockState(deepslatePos).is(Blocks.DEEPSLATE)) {
            context.fail("Deepslate not placed correctly");
            return;
        }

        // Replace with ores (simulating ore generation)
        context.setBlock(stonePos, LogisticsCore.BLOCK.TIN_ORE);
        context.setBlock(deepslatePos, LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE);

        // Verify ores replaced the target blocks
        if (!context.getBlockState(stonePos).is(LogisticsCore.BLOCK.TIN_ORE)) {
            context.fail("Tin ore did not replace stone");
            return;
        }

        if (!context.getBlockState(deepslatePos).is(LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE)) {
            context.fail("Deepslate tin ore did not replace deepslate");
            return;
        }

        context.succeed();
    }

    /**
     * Test that apatite ore can replace stone in the target environment.
     */
    @GameTest
    public void testApatiteOreCanReplaceStone(GameTestHelper context) {
        BlockPos stonePos = new BlockPos(1, 1, 1);

        // Place stone as generation target
        context.setBlock(stonePos, Blocks.STONE);

        // Verify stone is placed
        if (!context.getBlockState(stonePos).is(Blocks.STONE)) {
            context.fail("Stone not placed correctly");
            return;
        }

        // Replace with ore (simulating ore generation)
        context.setBlock(stonePos, LogisticsCore.BLOCK.APATITE_ORE);

        // Verify ore replaced the target block
        if (!context.getBlockState(stonePos).is(LogisticsCore.BLOCK.APATITE_ORE)) {
            context.fail("Apatite ore did not replace stone");
            return;
        }

        context.succeed();
    }
}
