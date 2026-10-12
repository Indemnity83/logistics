package com.logistics.forestry.logic;

import com.logistics.LogisticsForestry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Classifies a live world's blocks for {@link TreeFeller}. Never reads an unloaded position. */
public final class LevelTreeView implements TreeView {

    public static final TagKey<Block> FELLABLE_WOOD = tag("fellable_wood");
    public static final TagKey<Block> FELLABLE_CANOPY = tag("fellable_canopy");
    public static final TagKey<Block> NEVER_FELL = tag("never_fell");

    private final Level level;

    public LevelTreeView(Level level) {
        this.level = level;
    }

    @Override
    public Kind kind(BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return Kind.UNLOADED;
        }
        return classify(level.getBlockState(pos));
    }

    public static Kind classify(BlockState state) {
        if (state.hasBlockEntity() || state.is(NEVER_FELL)) {
            return Kind.NEVER_TOUCH;
        }
        if (state.is(FELLABLE_WOOD)) {
            return Kind.WOOD;
        }
        if (state.is(FELLABLE_CANOPY)) {
            boolean playerPlaced =
                    state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT);
            // a grounded propagule is a planted germling, not part of a canopy
            boolean grounded = state.is(Blocks.MANGROVE_PROPAGULE) && !state.getValue(MangrovePropaguleBlock.HANGING);
            return playerPlaced || grounded ? Kind.OTHER : Kind.CANOPY;
        }
        return Kind.OTHER;
    }

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK, LogisticsForestry.resource(name).toIdentifier());
    }
}
