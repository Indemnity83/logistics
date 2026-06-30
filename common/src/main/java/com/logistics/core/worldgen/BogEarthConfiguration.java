package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link BogEarthFeature}: how readily a placed seed spreads to adjacent soil
 * ({@code spread_chance}), the hard cap on a single patch ({@code max_count}), and how close a plant
 * must be for a seed to take ({@code plant_radius}).
 */
public record BogEarthConfiguration(float spreadChance, int maxCount, int plantRadius)
        implements FeatureConfiguration {

    public static final Codec<BogEarthConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("spread_chance").forGetter(BogEarthConfiguration::spreadChance),
            Codec.intRange(1, 4096).fieldOf("max_count").forGetter(BogEarthConfiguration::maxCount),
            Codec.intRange(0, 16).fieldOf("plant_radius").forGetter(BogEarthConfiguration::plantRadius))
        .apply(instance, BogEarthConfiguration::new));
}
