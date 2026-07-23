package com.logistics.power.engine.block;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Reaction Engine — reacts a liquid reactant with a solid catalyst to generate a huge, fixed burst of RF
 * that is pushed directly to the network with no buffer (unaccepted RF is lost). It cannot overheat and has
 * no wrench reset; the wrench only rotates it (inherited from {@link AbstractEngineBlock}). FACING sets the
 * output direction; POWERED gates <em>starting</em> a reaction (a running reaction ignores redstone).
 */
public class ReactionEngineBlock extends AbstractEngineBlock<ReactionEngineBlockEntity> {
    public static final MapCodec<ReactionEngineBlock> CODEC = simpleCodec(ReactionEngineBlock::new);

    public ReactionEngineBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactionEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type, LogisticsPower.ENTITY.REACTION_ENGINE_BLOCK_ENTITY, ReactionEngineBlockEntity::tick);
    }
}
