package com.logistics.gametest.core;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
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

    /** Fixed seed so a vein's shape — and therefore this test's outcome — is reproducible. */
    private static final long PLACEMENT_SEED = 0xC0FFEEL;

    // The solid volume an ore feature is asked to replace, and the origin it is placed from.
    // Deliberately small: GameTest cells are spawned six blocks apart, and OreFeature writes
    // straight into chunk sections rather than through the level, so a vein reaching past this
    // test's own cell would silently rewrite a concurrently running neighbour's blocks. With
    // PLACEMENT_SEED the tin veins occupy x 1-2, y 1-2, z 0-3 — comfortably inside. Re-measure
    // if either feature's size grows.
    private static final BlockPos HOST_VOLUME_MIN = new BlockPos(0, 1, 0);
    private static final BlockPos HOST_VOLUME_MAX = new BlockPos(4, 5, 4);
    private static final BlockPos PLACEMENT_ORIGIN = new BlockPos(2, 3, 2);

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
    public static void testApatiteOreConfiguredFeatureRegistered(GameTestHelper context) {
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
     * Runs the real tin ore feature against a solid stone volume and asserts tin ore appears.
     *
     * <p>The {@code ...Targets...} tests below only read the feature's declared replace rules
     * back out of the registry; this one is the end-to-end claim — that the configured feature
     * actually converts stone into tin ore when generation runs.
     */
    public static void testTinOreStoneFeatureGeneratesTinOre(GameTestHelper context) {
        generateOreIntoHostVolume(context, "tin_ore_stone", Blocks.STONE, LogisticsCore.BLOCK.TIN_ORE);
    }

    /**
     * Runs the real deepslate tin ore feature against a solid deepslate volume and asserts
     * deepslate tin ore appears.
     */
    public static void testTinOreDeepslateFeatureGeneratesDeepslateTinOre(GameTestHelper context) {
        generateOreIntoHostVolume(
            context, "tin_ore_deepslate", Blocks.DEEPSLATE, LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE);
    }

    /**
     * Test that tin ore (stone variant) targets stone blocks. Reads the configured feature's
     * replace rules back out of the registry — placement itself is covered by
     * {@link #testTinOreStoneFeatureGeneratesTinOre}.
     */
    public static void testTinOreTargetsStone(GameTestHelper context) {
        assertOreTargets(context, "tin_ore_stone", Blocks.STONE, LogisticsCore.BLOCK.TIN_ORE);
    }

    /**
     * Test that deepslate tin ore targets deepslate blocks. Reads the configured feature's
     * replace rules back out of the registry — placement itself is covered by
     * {@link #testTinOreDeepslateFeatureGeneratesDeepslateTinOre}.
     */
    public static void testTinOreTargetsDeepslate(GameTestHelper context) {
        assertOreTargets(context, "tin_ore_deepslate", Blocks.DEEPSLATE, LogisticsCore.BLOCK.DEEPSLATE_TIN_ORE);
    }

    /**
     * Test that apatite ore targets stone blocks.
     *
     * <p>Apatite has no placement counterpart on purpose: its {@code size: 48} vein spreads
     * roughly ten blocks from the origin, which is wider than a GameTest cell, so running it
     * here would rewrite whatever a neighbouring test had placed.
     */
    public static void testApatiteOreTargetsStone(GameTestHelper context) {
        assertOreTargets(context, "apatite_ore_stone", Blocks.STONE, LogisticsCore.BLOCK.APATITE_ORE);
    }

    /**
     * Fills {@link #HOST_VOLUME_MIN}..{@link #HOST_VOLUME_MAX} with {@code host}, runs the named
     * configured feature from the level's own registry, and asserts the volume now holds the
     * expected ore and nothing else.
     */
    private static void generateOreIntoHostVolume(
            GameTestHelper context, String featureName, Block host, Block expectedOre) {
        ConfiguredFeature<?, ?> feature = configuredFeature(context, featureName);
        if (feature == null) {
            return;
        }

        for (BlockPos pos : BlockPos.betweenClosed(HOST_VOLUME_MIN, HOST_VOLUME_MAX)) {
            context.setBlock(pos, host);
        }

        ServerLevel level = context.getLevel();
        boolean placed = feature.place(
            level,
            level.getChunkSource().getGenerator(),
            RandomSource.create(PLACEMENT_SEED),
            context.absolutePos(PLACEMENT_ORIGIN)
        );
        if (!placed) {
            context.fail(featureName + " generated nothing inside a solid " + blockName(host) + " volume");
            return;
        }

        int oreBlocks = 0;
        for (BlockPos pos : BlockPos.betweenClosed(HOST_VOLUME_MIN, HOST_VOLUME_MAX)) {
            BlockState state = context.getBlockState(pos);
            if (state.is(expectedOre)) {
                oreBlocks++;
            } else if (!state.is(host)) {
                context.fail(featureName + " replaced " + blockName(host) + " at " + pos + " with "
                    + blockName(state.getBlock()) + " instead of " + blockName(expectedOre));
                return;
            }
        }

        if (oreBlocks == 0) {
            context.fail(featureName + " left no " + blockName(expectedOre) + " in the " + blockName(host)
                + " volume it generated into");
            return;
        }

        context.succeed();
    }

    /**
     * Asserts the named ore configuration declares a target that accepts {@code host} and places
     * {@code expectedOre}.
     */
    private static void assertOreTargets(
            GameTestHelper context, String featureName, Block host, Block expectedOre) {
        ConfiguredFeature<?, ?> feature = configuredFeature(context, featureName);
        if (feature == null) {
            return;
        }

        if (!(feature.config() instanceof OreConfiguration oreConfig)) {
            context.fail(featureName + " is not an OreConfiguration");
            return;
        }

        for (OreConfiguration.TargetBlockState target : oreConfig.targetStates) {
            if (!target.target.test(host.defaultBlockState(), context.getLevel().getRandom())) {
                continue;
            }
            if (!target.state.is(expectedOre)) {
                context.fail(featureName + " replaces " + blockName(host) + " with "
                    + blockName(target.state.getBlock()) + " instead of " + blockName(expectedOre));
                return;
            }
            context.succeed();
            return;
        }

        context.fail(featureName + " does not target " + blockName(host));
    }

    /** Looks a mod configured feature up in the level's registry, failing the test if absent. */
    private static ConfiguredFeature<?, ?> configuredFeature(GameTestHelper context, String featureName) {
        ResourceKey<ConfiguredFeature<?, ?>> featureKey = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            LogisticsMod.modId(featureName).toIdentifier()
        );

        var registry = context.getLevel().registryAccess().lookup(Registries.CONFIGURED_FEATURE);
        if (registry.isEmpty()) {
            context.fail("Configured feature registry not available");
            return null;
        }

        var featureHolder = registry.get().get(featureKey);
        if (featureHolder.isEmpty()) {
            context.fail(featureName + " configured feature not found");
            return null;
        }

        return featureHolder.get().value();
    }

    private static String blockName(Block block) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
    }
}
