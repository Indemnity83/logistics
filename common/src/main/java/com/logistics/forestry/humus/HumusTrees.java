package com.logistics.forestry.humus;

import com.logistics.LogisticsForestry;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import org.jetbrains.annotations.Nullable;

/**
 * The world rule behind Humus: a germling standing on it grows {@link HumusTreeShape}'s small tree in
 * place of its vanilla feature, one germling at a time, and never grows otherwise. Each loader routes
 * its tree growth here.
 */
public final class HumusTrees {

    /** The flags vanilla's tree grower lifts a sapling with before placing its tree. */
    private static final int LIFT_GERMLING_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE
            | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
            | Block.UPDATE_SKIP_ON_PLACE;

    /** The flags vanilla's tree grower puts a sapling back with when its tree did not fit. */
    private static final int RESTORE_GERMLING_FLAGS =
            Block.UPDATE_INVISIBLE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private HumusTrees() {}

    /** Whether the germling at {@code pos} stands on Humus, so grows the Humus tree. */
    public static boolean growsHumusTree(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.SAPLINGS)
                && level.getBlockState(pos.below()).is(LogisticsForestry.BLOCK.HUMUS);
    }

    /**
     * Grows the Humus tree for {@code definition}, the germling's own tree definition, in place of
     * {@code germling} at {@code pos}. Without a tree definition, or without room for the tree, the
     * germling stays and this returns false.
     */
    public static boolean grow(
            ServerLevel level,
            ChunkGenerator generator,
            BlockPos pos,
            BlockState germling,
            RandomSource random,
            @Nullable Feature definition) {
        Optional<TreeFeature> tree = HumusTreeShape.of(definition);
        if (tree.isEmpty()) {
            return false;
        }
        level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), LIFT_GERMLING_FLAGS);
        if (tree.get().place(level, generator, random, pos)) {
            return true;
        }
        level.setBlock(pos, germling, RESTORE_GERMLING_FLAGS);
        return false;
    }
}
