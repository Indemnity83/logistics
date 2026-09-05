package com.logistics.core.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.core.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.LogisticsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Redstone Engine - converts redstone signals to MJ energy.
 * The simplest engine type that outputs 0.05 MJ/t (1 MJ/s) when powered.
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>FACING property determines output direction (where energy is pushed)</li>
 *   <li>Only responds to direct redstone signals (levers, buttons) not dust</li>
 *   <li>Has a small internal buffer (10 MJ) and stalls when full</li>
 * </ul>
 */
public class RedstoneEngineBlock extends AbstractEngineBlock<RedstoneEngineBlockEntity> {
    public RedstoneEngineBlock(Properties settings) {
        super(settings);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, LogisticsCore.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, RedstoneEngineBlockEntity::tick);
    }
}
