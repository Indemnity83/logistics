package com.logistics.power.block.entity;

import com.logistics.api.EnergyStorage;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.power.registry.PowerBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Block entity for the Creative Sink.
 * Accepts energy from all sides and discards it at a configurable rate.
 * Useful for testing engine output and PID tuning.
 */
public class CreativeSinkBlockEntity extends BlockEntity implements EnergyStorage, AcceptsLowTierEnergy {
    private static final long[] DRAIN_RATES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100};
    private int drainRateIndex = 4; // Default 5 RF/t
    private long energyThisTick = 0;

    public CreativeSinkBlockEntity(BlockPos pos, BlockState state) {
        super(PowerBlockEntities.CREATIVE_SINK_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CreativeSinkBlockEntity entity) {
        // Reset energy counter each tick - energy is discarded
        entity.energyThisTick = 0;
    }

    public long getDrainRate() {
        return DRAIN_RATES[drainRateIndex];
    }

    public void increaseDrainRate() {
        drainRateIndex = Math.min(drainRateIndex + 1, DRAIN_RATES.length - 1);
        markDirty();
    }

    public void decreaseDrainRate() {
        drainRateIndex = Math.max(drainRateIndex - 1, 0);
        markDirty();
    }

    /**
     * Cycles to the next drain rate, looping back to the start.
     *
     * @return the new drain rate
     */
    public long cycleDrainRate() {
        drainRateIndex = (drainRateIndex + 1) % DRAIN_RATES.length;
        markDirty();
        return DRAIN_RATES[drainRateIndex];
    }

    /**
     * Returns probe diagnostic information.
     */
    public ProbeResult getProbeResult() {
        return ProbeResult.builder("Creative Sink Stats")
                .entry("Drain Rate", String.format("%d RF/t", getDrainRate()), Formatting.AQUA)
                .entry("Energy Received", String.format("%d / %d RF", energyThisTick, getDrainRate()), Formatting.GREEN)
                .build();
    }

    // ==================== EnergyStorage Implementation ====================

    @Override
    public long getAmount() {
        return 0; // Always empty - we discard energy
    }

    @Override
    public long getCapacity() {
        return getDrainRate(); // Capacity = drain rate for display purposes
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        long canAccept = Math.max(0, getDrainRate() - energyThisTick);
        long toAccept = Math.min(maxAmount, canAccept);
        if (!simulate && toAccept > 0) {
            energyThisTick += toAccept;
        }
        return toAccept;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        return 0; // Cannot extract
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        NbtCompound data = new NbtCompound();
        data.putInt("drainRateIndex", drainRateIndex);
        view.put("CreativeSink", NbtCompound.CODEC, data);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        view.read("CreativeSink", NbtCompound.CODEC).ifPresent(data -> {
            drainRateIndex = data.getInt("drainRateIndex").orElse(4);
            // Clamp to valid range
            if (drainRateIndex < 0 || drainRateIndex >= DRAIN_RATES.length) {
                drainRateIndex = 4; // Default to 5 RF/t
            }
        });
    }
}
