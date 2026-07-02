package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link OilDepositFeature}: a speckled oil-ore blob of a semi-random radius between
 * {@code minRadius} and {@code maxRadius}, replacing solid ground (never air/fluid) with the biome's bank.
 */
public record OilDepositConfiguration(int minRadius, int maxRadius) implements FeatureConfiguration {

    public static final Codec<OilDepositConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 48).fieldOf("min_radius").forGetter(OilDepositConfiguration::minRadius),
            Codec.intRange(1, 48).fieldOf("max_radius").forGetter(OilDepositConfiguration::maxRadius))
        .apply(instance, OilDepositConfiguration::new));
}
