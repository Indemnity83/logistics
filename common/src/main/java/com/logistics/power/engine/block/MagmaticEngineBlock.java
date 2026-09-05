package com.logistics.power.engine.block;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Magmatic Engine — burns liquid lava as a fixed-duration heat source, heat-soaking from Cold → Warm → Hot
 * and generating continuously with temperature (5–15 RF/t). It cannot overheat, needs no coolant, and has
 * no wrench reset; the wrench only rotates it (inherited from {@link AbstractEngineBlock}). FACING sets the
 * output direction; POWERED gates operation.
 */
public class MagmaticEngineBlock extends AbstractEngineBlock<MagmaticEngineBlockEntity> {
    public MagmaticEngineBlock(Properties settings) {
        super(settings);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagmaticEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(
                type, LogisticsPower.ENTITY.MAGMATIC_ENGINE_BLOCK_ENTITY, MagmaticEngineBlockEntity::tick);
    }
}
