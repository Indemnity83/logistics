package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.power.LowTierEnergySource;
import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.LogisticsPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for the Redstone Engine.
 * The simplest engine type that converts redstone signals to energy.
 */
public class RedstoneEngineBlockEntity extends AbstractEngineBlockEntity implements LowTierEnergySource {
    private static final long MAX_ENERGY = 1000L;

    // Energy generation: +10 RF every 16 ticks when powered
    private static final int ENERGY_TICK_INTERVAL = 16;
    private static final long ENERGY_PER_INTERVAL = 10L;

    public RedstoneEngineBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RedstoneEngineBlockEntity entity) {
        entity.tickEngine(level, pos, state);
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
        return RedstoneEngineBlock.getOutputDirection(getBlockState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getBlockState().getValue(RedstoneEngineBlock.POWERED);
    }

    @Override
    public boolean isRunning() {
        if (!super.isRunning()) {
            return false;
        }

        // Don't run if target doesn't accept low-tier energy
        if (level == null) {
            return false;
        }

        Direction outputDir = getOutputDirection();
        BlockPos targetPos = worldPosition.relative(outputDir);
        BlockEntity target = level.getBlockEntity(targetPos);

        if (target instanceof AcceptsLowTierEnergy acceptor) {
            return acceptor.acceptsLowTierEnergyFrom(outputDir.getOpposite());
        }

        return false; // Target doesn't accept low-tier energy
    }

    @Override
    protected void produceEnergy() {
        if (!isRedstonePowered() || level == null) {
            return;
        }

        if (level.getGameTime() % ENERGY_TICK_INTERVAL == 0) {
            addEnergy(ENERGY_PER_INTERVAL);
        }
    }
}
