package com.logistics.power.engine.block.entity;

import com.logistics.power.engine.block.RedstoneEngineBlock;
import com.logistics.power.registry.PowerBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Block entity for the Redstone Engine.
 * The simplest engine type that converts redstone signals to energy.
 *
 * BuildCraft-aligned mechanics:
 * - Heat is derived from energy level: heat = (MAX_HEAT - MIN_HEAT) * energyLevel + MIN_HEAT
 * - Stage thresholds based on energy level (not heat):
 *   - BLUE: 0-33% energy
 *   - GREEN: 33-66% energy
 *   - YELLOW: 66-75% energy
 *   - RED: 75%+ energy
 * - Energy generation: +10 RF every 16 ticks when redstone powered (independent of piston)
 * - Energy decay: -10 RF/tick when not powered
 * - Piston speeds by stage: BLUE=0.01, GREEN=0.02, YELLOW=0.04, RED=0.08
 * - Visual oscillation: When in RED stage, shows YELLOW during expansion (progress < 0.5)
 *   and RED during compression (progress >= 0.5) - the iconic "breathing" effect
 */
public class RedstoneEngineBlockEntity extends AbstractEngineBlockEntity {
    // Energy buffer (1000 RF, matching BuildCraft)
    private static final long MAX_ENERGY = 1000L;

    // Energy generation: +10 RF every 16 ticks when powered (BuildCraft pattern)
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

    /**
     * Computes stage based on energy level (not heat).
     * Redstone engines cannot overheat - they simply cap at RED stage.
     *
     * BuildCraft thresholds:
     * - BLUE: 0-33% energy
     * - GREEN: 33-66% energy
     * - YELLOW: 66-75% energy
     * - RED: 75%+ energy
     */
    @Override
    protected HeatStage computeStage() {
        double energyLevel = getEnergyLevel();

        if (energyLevel < 0.33) return HeatStage.COLD;
        if (energyLevel < 0.66) return HeatStage.COOL;
        if (energyLevel < 0.75) return HeatStage.WARM;
        return HeatStage.HOT;
    }

    @Override
    protected Direction getOutputDirection() {
        return RedstoneEngineBlock.getOutputDirection(getCachedState());
    }

    @Override
    protected boolean isRedstonePowered() {
        return getCachedState().get(RedstoneEngineBlock.POWERED);
    }

    /**
     * Produces energy from redstone signal: +10 RF every 16 ticks when powered.
     * Uses world time for simplicity (BuildCraft pattern).
     */
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
