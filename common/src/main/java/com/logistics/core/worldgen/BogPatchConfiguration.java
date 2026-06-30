package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link BogPatchFeature}: which blocks the mud→bog flood-fill seeds on and spreads through
 * ({@code target}), the per-neighbour {@code spread_chance}, and the {@code max_count} cap on one patch.
 */
public record BogPatchConfiguration(BlockPredicate target, float spreadChance, int maxCount)
        implements FeatureConfiguration {

    public static final Codec<BogPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPredicate.CODEC.fieldOf("target").forGetter(BogPatchConfiguration::target),
            Codec.floatRange(0.0f, 1.0f).fieldOf("spread_chance").forGetter(BogPatchConfiguration::spreadChance),
            Codec.intRange(1, 4096).fieldOf("max_count").forGetter(BogPatchConfiguration::maxCount))
        .apply(instance, BogPatchConfiguration::new));
}
