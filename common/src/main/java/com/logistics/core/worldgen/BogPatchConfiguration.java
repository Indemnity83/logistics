package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Config for {@link BogPatchFeature}. Mirrors vanilla {@code DiskConfiguration} (state_provider / target
 * / radius / half_height) — but the feature unions several overlapping lobes within {@code radius} for an
 * irregular outline instead of one clean circle.
 */
public record BogPatchConfiguration(
        BlockStateProvider stateProvider, BlockPredicate target, IntProvider radius, int halfHeight)
        implements FeatureConfiguration {

    public static final Codec<BogPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("state_provider").forGetter(BogPatchConfiguration::stateProvider),
            BlockPredicate.CODEC.fieldOf("target").forGetter(BogPatchConfiguration::target),
            IntProviders.codec(1, 16).fieldOf("radius").forGetter(BogPatchConfiguration::radius),
            Codec.intRange(0, 4).fieldOf("half_height").forGetter(BogPatchConfiguration::halfHeight))
        .apply(instance, BogPatchConfiguration::new));
}
