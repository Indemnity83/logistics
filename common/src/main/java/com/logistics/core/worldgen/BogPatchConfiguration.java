package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link BogPatchFeature}: the per-neighbour {@code spread_chance} of the mud→bog flood-fill
 * and the {@code max_count} hard cap on a single patch.
 */
public record BogPatchConfiguration(float spreadChance, int maxCount) implements FeatureConfiguration {

    public static final Codec<BogPatchConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("spread_chance").forGetter(BogPatchConfiguration::spreadChance),
            Codec.intRange(1, 4096).fieldOf("max_count").forGetter(BogPatchConfiguration::maxCount))
        .apply(instance, BogPatchConfiguration::new));
}
