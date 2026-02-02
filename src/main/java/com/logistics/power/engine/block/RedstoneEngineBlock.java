package com.logistics.power.engine.block;

import com.logistics.core.lib.power.AbstractEngineBlock;
import com.logistics.power.engine.block.entity.RedstoneEngineBlockEntity;
import com.logistics.power.registry.PowerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    public static final MapCodec<RedstoneEngineBlock> CODEC = createCodec(RedstoneEngineBlock::new);

    public RedstoneEngineBlock(Settings settings) {
        super(settings, BlockSoundGroup.STONE);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected RedstoneEngineBlockEntity getEngineBlockEntity(BlockEntity be) {
        return be instanceof RedstoneEngineBlockEntity ? (RedstoneEngineBlockEntity) be : null;
    }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneEngineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, RedstoneEngineBlockEntity::tick);
    }
}
