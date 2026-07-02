package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link OilSeepFeature}: a surface crude oil pool. {@code fluid} fills an organic bowl of
 * {@code radius}/{@code depth} (jittered per seep). The basin is lined with the biome's oil ore, which
 * the feature picks itself (oil sand in deserts, oil red sand in badlands, oil shale elsewhere): every
 * air-adjacent face is lined so the pool can't leak, while each solid face becomes ore only at
 * {@code shellChance}, so the banks read as oil-soaked patches rather than a full casing.
 */
public record OilSeepConfiguration(BlockState fluid, int radius, int depth, float shellChance)
        implements FeatureConfiguration {

    public static final Codec<OilSeepConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("fluid").forGetter(OilSeepConfiguration::fluid),
            Codec.intRange(1, 16).fieldOf("radius").forGetter(OilSeepConfiguration::radius),
            Codec.intRange(1, 16).fieldOf("depth").forGetter(OilSeepConfiguration::depth),
            Codec.floatRange(0.0f, 1.0f).fieldOf("shell_chance").forGetter(OilSeepConfiguration::shellChance))
        .apply(instance, OilSeepConfiguration::new));
}
