package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link OilPondFeature}: the blocks the pond may replace ({@code target} — sand + sandstone
 * for deserts, the red variants for badlands), the {@code result} it turns them into, and the pond's base
 * {@code radius} (half-width) and {@code depth} (layers carved down). The feature jitters both per pond
 * and lobes the edge / tapers the depth, so no two ponds share a shape. Only {@code target} blocks are
 * replaced, so the pond stops at the stone under the sand.
 */
public record OilPondConfiguration(BlockPredicate target, BlockState result, int radius, int depth)
        implements FeatureConfiguration {

    public static final Codec<OilPondConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPredicate.CODEC.fieldOf("target").forGetter(OilPondConfiguration::target),
            BlockState.CODEC.fieldOf("result").forGetter(OilPondConfiguration::result),
            Codec.intRange(1, 16).fieldOf("radius").forGetter(OilPondConfiguration::radius),
            Codec.intRange(1, 32).fieldOf("depth").forGetter(OilPondConfiguration::depth))
        .apply(instance, OilPondConfiguration::new));
}
