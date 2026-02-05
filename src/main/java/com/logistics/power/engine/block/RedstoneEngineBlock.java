package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
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
    public static final MapCodec<RedstoneEngineBlock> CODEC = simpleCodec(RedstoneEngineBlock::new);

    public RedstoneEngineBlock(Properties settings) {
        super(settings, SoundType.STONE);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RedstoneEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof RedstoneEngineBlockEntity ? (RedstoneEngineBlockEntity) be : null;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, RedstoneEngineBlockEntity::tick);
    }
}
