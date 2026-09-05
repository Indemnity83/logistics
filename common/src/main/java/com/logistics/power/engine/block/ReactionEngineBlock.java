package com.logistics.power.engine.block;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
    public ReactionEngineBlock(Properties settings) {
        super(settings);
    }

    /** Place in the "off" (dark) tint immediately; the tick flips it to HOT once a reaction starts. */
    @Override
    protected BlockState applyAdditionalPlacementState(BlockState base, BlockPlaceContext ctx) {
        return base.setValue(HeatStage.STAGE, HeatStage.OVERHEAT);
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
