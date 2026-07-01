package com.logistics.core.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Carves a large, deep "pond" of oil-soaked ground: over a circular {@code radius} around the (surface)
 * origin, it walks each column down to {@code depth} layers and turns any {@code target} block (sand +
 * sandstone, or the red variants) into the config's {@code result}. Because only target blocks are
 * replaced, the pond fills the sand/sandstone column and naturally bottoms out on the stone beneath —
 * unlike vanilla's {@code disk} feature, which caps its half-height at 4.
 */
public class OilPondFeature extends Feature<OilPondConfiguration> {

    public OilPondFeature(Codec<OilPondConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OilPondConfiguration> context) {
        WorldGenLevel level = context.level();
        OilPondConfiguration config = context.config();
        BlockPredicate target = config.target();
        BlockState result = config.result();
        int radius = config.radius();
        int depth = config.depth();

        // The heightmap origin sits just above the surface, so the top ground block is one below it.
        BlockPos origin = context.origin();
        int topY = origin.getY() - 1;
        int radiusSq = radius * radius;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                for (int dy = 0; dy < depth; dy++) {
                    pos.set(origin.getX() + dx, topY - dy, origin.getZ() + dz);
                    if (target.test(level, pos)) {
                        level.setBlock(pos, result, 2);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }
}
