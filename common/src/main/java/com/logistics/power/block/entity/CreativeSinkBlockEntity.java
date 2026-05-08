package com.logistics.power.block.entity;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.block.behavior.ProbeResult;
import com.logistics.core.lib.power.EnergyDemandProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.logistics.core.lib.energy.IEnergyStorage;

/**
 * Block entity for the Creative Sink.
 * Accepts energy from all sides and discards it at a configurable rate.
 * Useful for testing engine output and PID tuning.
 * <p>
 * Note: Implements {@link HasEnergyStorage} with custom storage that discards energy
 * rather than storing it. This demonstrates that the trait can be implemented with
 * custom logic instead of using {@link com.logistics.core.lib.energy.EnergyComponent}.
 */
public class CreativeSinkBlockEntity extends BaseBlockEntity
    implements AcceptsLowTierEnergy, HasEnergyStorage, EnergyDemandProvider {
    private static final long[] DRAIN_RATES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100, Long.MAX_VALUE};
    private int drainRateIndex = 4; // Default 5 RF/t
    private long energyLastTick = 0;
    private long energyThisTick = 0;

    // Test-only counter - not persisted, resets on reload
    // Used only for validating energy consumption in tests
    long totalEnergyReceived = 0;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long canAccept = Math.max(0, getDrainRate() - energyThisTick);
            long toAccept = Math.min(maxAmount, canAccept);
            if (toAccept > 0 && !simulate) {
                energyThisTick += toAccept;
            }
            return toAccept;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
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

        @Override
        public boolean canExtract() {
            return false;
        }
    };

    public CreativeSinkBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsPower.ENTITY.CREATIVE_SINK_BLOCK_ENTITY, pos, state);
    }

    // ==================== HasEnergyStorage ====================

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        // Creative sink accepts energy from all sides
        return energyStorage;
    }

    // ==================== Drain Rate & Stats ====================

    public static void tick(Level world, BlockPos pos, BlockState state, CreativeSinkBlockEntity entity) {
        // Reset energy counter each tick - energy is discarded
        entity.energyLastTick = entity.energyThisTick;

        // Accumulate total energy with saturation to prevent overflow
        // When at Long.MAX_VALUE drain rate, this could otherwise wrap around
        if (entity.energyThisTick > 0 && Long.MAX_VALUE - entity.totalEnergyReceived <= entity.energyThisTick) {
            entity.totalEnergyReceived = Long.MAX_VALUE;
        } else {
            entity.totalEnergyReceived += entity.energyThisTick;
        }

        entity.energyThisTick = 0;
    }

    public long getDrainRate() {
        return DRAIN_RATES[drainRateIndex];
    }

    @Override
    public long networkDemandPerTick() {
        long drainRate = getDrainRate();
        if (drainRate == Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(0, drainRate - energyThisTick);
    }

    /**
     * Cycles to the next drain rate, looping back to the start.
     *
     * @return the new drain rate
     */
    public long cycleDrainRate() {
        drainRateIndex = (drainRateIndex + 1) % DRAIN_RATES.length;
        markDirtyAndSync(); // Sync to clients for potential UI/tooltip updates
        return DRAIN_RATES[drainRateIndex];
    }

    /**
     * Sets the drain rate to unlimited.
     * <p>
     * <b>Testing API:</b> This method is primarily intended for game tests.
     * In production, use {@link #cycleDrainRate()} to cycle through drain rates.
     * </p>
     * This allows the sink to accept any amount of energy per tick.
     */
    public void setUnlimitedDrainRate() {
        drainRateIndex = DRAIN_RATES.length - 1; // Last index is Long.MAX_VALUE
        markDirtyAndSync();
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
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);
        nbt.putInt("DrainRateIndex", drainRateIndex);
        // Note: totalEnergyReceived not persisted - testing-only counter, resets on reload
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);
        drainRateIndex = NbtCompat.getInt(nbt, "DrainRateIndex", 4);
        // Clamp to valid range
        if (drainRateIndex < 0 || drainRateIndex >= DRAIN_RATES.length) {
            drainRateIndex = 4; // Default to 5 RF/t
        }
        // Note: totalEnergyReceived not loaded - testing-only counter, starts at 0
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view);
        view.read("CreativeSink", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(data -> {
            // Load old key name (before BaseBlockEntity refactoring)
            drainRateIndex = NbtCompat.getInt(data, "drainRateIndex", 4);
            // Clamp to valid range
            if (drainRateIndex < 0 || drainRateIndex >= DRAIN_RATES.length) {
                drainRateIndex = 4; // Default to 5 RF/t
            }
        });
    }
}
