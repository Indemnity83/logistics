package com.logistics.pipe.block;

import com.logistics.LogisticsFluid;
import com.logistics.pipe.block.entity.FluidPumpBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FluidPumpBlock extends BaseEntityBlock {
    public static final MapCodec<FluidPumpBlock> CODEC = simpleCodec(FluidPumpBlock::new);

    public FluidPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // Ticks on both sides: the client uses it to interpolate the descending tube toward targetY.
        return createTickerHelper(type, LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY, FluidPumpBlockEntity::tick);
    }
}
