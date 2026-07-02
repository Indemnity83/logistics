package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link OilSandsFeature}: a speckled oil-sands blob of a semi-random radius between
 * {@code minRadius} and {@code maxRadius}, replacing solid ground (never air/fluid) with the biome's bank.
 * {@code maxY} caps the surface height a deposit may spawn at, keeping them off hills and high badlands mesas.
 */
public record OilSandsConfiguration(int minRadius, int maxRadius, int maxY) implements FeatureConfiguration {

    public static final Codec<OilSandsConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 48).fieldOf("min_radius").forGetter(OilSandsConfiguration::minRadius),
            Codec.intRange(1, 48).fieldOf("max_radius").forGetter(OilSandsConfiguration::maxRadius),
            Codec.intRange(-64, 320).fieldOf("max_y").forGetter(OilSandsConfiguration::maxY))
        .apply(instance, OilSandsConfiguration::new));
}
