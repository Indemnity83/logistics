package com.logistics.power.block.entity;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.block.BaseBlockEntity;
import com.logistics.core.lib.block.behavior.ProbeResult;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import com.logistics.core.lib.power.EnergyDemandProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
    private final CreativeSinkDrainState drainState = new CreativeSinkDrainState();

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            return drainState.insert(maxAmount, simulate);
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
        entity.drainState.tick();
    }

    public long getDrainRate() {
        return drainState.drainRate();
    }

    @Override
    public long networkDemandPerTick() {
        return drainState.networkDemandPerTick();
    }

    /**
     * Cycles to the next drain rate, looping back to the start.
     *
     * @return the new drain rate
     */
    public long cycleDrainRate() {
        long drainRate = drainState.cycle();
        markDirtyAndSync(); // Sync to clients for potential UI/tooltip updates
        return drainRate;
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
        drainState.setUnlimited();
        markDirtyAndSync();
    }

    /**
     * Returns probe diagnostic information.
     */
    public ProbeResult getProbeResult() {
        return ProbeResult.builder("Creative Sink Stats")
                .entry("Drain Rate", String.format("%d RF/t", getDrainRate()), ChatFormatting.AQUA)
                .entry("Energy Received", String.format("%d RF", drainState.energyLastTick()), ChatFormatting.GREEN)
                .build();
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveLogisticsData(nbt, registries);
        nbt.putInt("DrainRateIndex", drainState.index());
        // Note: totalEnergyReceived not persisted - testing-only counter, resets on reload
    }

    @Override
    protected void loadLogisticsData(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadLogisticsData(nbt, registries);
        drainState.restore(NbtCompat.getInt(nbt, "DrainRateIndex", 4));
        // Note: totalEnergyReceived not loaded - testing-only counter, starts at 0
    }

    @Override
    protected void loadLegacyData(net.minecraft.world.level.storage.ValueInput view) {
        super.loadLegacyData(view);
        view.read("CreativeSink", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(data -> {
            // Load old key name (before BaseBlockEntity refactoring)
            drainState.restore(NbtCompat.getInt(data, "drainRateIndex", 4));
        });
    }
}
