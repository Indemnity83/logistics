package com.logistics.power.block.entity;

import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import com.logistics.core.lib.support.ProbeResult;
import com.logistics.LogisticsPower;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import team.reborn.energy.api.EnergyStorage;

/**
 * Block entity for the Creative Sink.
 * Accepts energy from all sides and discards it at a configurable rate.
 * Useful for testing engine output and PID tuning.
 */
public class CreativeSinkBlockEntity extends BlockEntity implements AcceptsLowTierEnergy {
    private static final long[] DRAIN_RATES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100};
    private int drainRateIndex = 4; // Default 5 RF/t
    private long energyLastTick = 0;
    private long energyThisTick = 0;

    public final EnergyStorage energyStorage = new EnergyStorage() {
        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            long canAccept = Math.max(0, getDrainRate() - energyThisTick);
            long toAccept = Math.min(maxAmount, canAccept);
            if (toAccept > 0) {
                energyThisTick += toAccept;
            }
            return toAccept;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return Long.MAX_VALUE;
        }
    };

    public CreativeSinkBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.CREATIVE_SINK_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CreativeSinkBlockEntity entity) {
        // Reset energy counter each tick - energy is discarded
        entity.energyLastTick = entity.energyThisTick;
        entity.energyThisTick = 0;
    }

    public long getDrainRate() {
        return DRAIN_RATES[drainRateIndex];
    }

    /**
     * Cycles to the next drain rate, looping back to the start.
     *
     * @return the new drain rate
     */
    public long cycleDrainRate() {
        drainRateIndex = (drainRateIndex + 1) % DRAIN_RATES.length;
        setChanged();
        return DRAIN_RATES[drainRateIndex];
    }

    /**
     * Returns probe diagnostic information.
     */
    public ProbeResult getProbeResult() {
        return ProbeResult.builder("Creative Sink Stats")
                .entry("Drain Rate", String.format("%d RF/t", getDrainRate()), ChatFormatting.AQUA)
                .entry("Energy Received", String.format("%d RF", energyLastTick), ChatFormatting.GREEN)
                .build();
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveAdditional(ValueOutput view) {
        CompoundTag data = new CompoundTag();
        data.putInt("drainRateIndex", drainRateIndex);
        view.store("CreativeSink", CompoundTag.CODEC, data);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        view.read("CreativeSink", CompoundTag.CODEC).ifPresent(data -> {
            drainRateIndex = data.getInt("drainRateIndex").orElse(4);
            // Clamp to valid range
            if (drainRateIndex < 0 || drainRateIndex >= DRAIN_RATES.length) {
                drainRateIndex = 4; // Default to 5 RF/t
            }
        });
    }
}
