package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.power.registry.PowerBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Block entity for the Redstone Engine.
 * The simplest engine type that converts redstone signals to energy.
 */
public class RedstoneEngineBlockEntity extends AbstractEngineBlockEntity {
    private static final long MAX_ENERGY = 1000L;

    // Energy generation: +10 RF every 16 ticks when powered
    private static final int ENERGY_TICK_INTERVAL = 16;
    private static final long ENERGY_PER_INTERVAL = 10L;

    public RedstoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RedstoneEngineBlockEntity entity) {
        entity.tickEngine(world, pos, state);
    }

    @Override
    protected long getEnergyBufferCapacity() {
        return MAX_ENERGY;
    }

    @Override
    protected long getOutputPower() {
        return 10L;
    }

    @Override
    public boolean canOverheat() {
        return false;
    }

    @Override
    protected Direction getOutputDirection() {
        return RedstoneEngineBlock.getOutputDirection(getCachedState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getCachedState().get(RedstoneEngineBlock.POWERED);
    }

    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered() || world == null) {
            return;
        }

        if (world.getTime() % ENERGY_TICK_INTERVAL == 0) {
            addEnergy(ENERGY_PER_INTERVAL);
        }
    }
}
