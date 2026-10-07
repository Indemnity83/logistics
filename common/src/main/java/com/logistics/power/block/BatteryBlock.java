package com.logistics.power.block;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.AbstractBatteryBlockEntity;
import com.logistics.power.block.entity.BatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * Energy buffer block. Accepts power from generators and supplies it to adjacent
 * machines and connected logistics pipe networks.
 *
 * <p>The tier is baked into the {@link Block} instance rather than the block state, so the five
 * grades are five blocks sharing one block entity type — the same arrangement
 * {@link com.logistics.power.cable.CableBlock} uses.
 */
public class BatteryBlock extends BaseEntityBlock {

    private final BatteryTier tier;

    public BatteryBlock(Properties properties, BatteryTier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(AbstractBatteryBlockEntity.CHARGE, 0));
    }

    public BatteryTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AbstractBatteryBlockEntity.CHARGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BatteryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, LogisticsPower.ENTITY.BATTERY_BLOCK_ENTITY,
                AbstractBatteryBlockEntity::tick);
    }
}
