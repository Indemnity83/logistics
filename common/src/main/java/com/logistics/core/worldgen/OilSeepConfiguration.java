package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link OilSeepFeature}: a surface crude oil pool with an oil-ore shell and radiating
 * tendrils. {@code fluid} fills the bowl (crude oil), {@code shell} lines the basin and forms the
 * tendrils (the biome's oil sand / oil red sand). {@code radius} + {@code depth} size the bowl (jittered
 * per seep); {@code tendrilChance} is the per-step continuation probability of each radiating streak.
 */
public record OilSeepConfiguration(BlockState fluid, BlockState shell, int radius, int depth, float tendrilChance)
        implements FeatureConfiguration {

    public static final Codec<OilSeepConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("fluid").forGetter(OilSeepConfiguration::fluid),
            BlockState.CODEC.fieldOf("shell").forGetter(OilSeepConfiguration::shell),
            Codec.intRange(1, 16).fieldOf("radius").forGetter(OilSeepConfiguration::radius),
            Codec.intRange(1, 16).fieldOf("depth").forGetter(OilSeepConfiguration::depth),
            Codec.floatRange(0.0f, 1.0f).fieldOf("tendril_chance").forGetter(OilSeepConfiguration::tendrilChance))
        .apply(instance, OilSeepConfiguration::new));
}
