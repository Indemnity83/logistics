package com.logistics.core.lib.block;

import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.EnergyDemandProvider;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Base for RF-powered machines. Owns an internal energy buffer that charges from the network — even
 * while idle — and reports its free room as demand. Subclasses own their inventory, recipes, and GUI.
 */
public abstract class MachineBlockEntity extends BaseBlockEntity implements HasEnergyStorage, EnergyDemandProvider {

    protected final EnergyComponent energy;
    private final LongSupplier maxEnergyInput;
    private long energyReceivedThisTick = 0;

    // Wraps the buffer to count RF accepted this tick, so demand reflects the remaining input budget.
    private final IEnergyStorage trackingStorage = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long inserted = energy.insert(maxAmount, simulate);
            if (!simulate && inserted > 0) energyReceivedThisTick += inserted;
            return inserted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return energy.extract(maxAmount, simulate);
        }

        @Override
        public long getAmount() {
            return energy.getAmount();
        }

        @Override
        public long getCapacity() {
            return energy.getCapacity();
        }

        @Override
        public boolean canInsert() {
            return energy.canInsert();
        }

        @Override
        public boolean canExtract() {
            return energy.canExtract();
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            LongSupplier energyCapacity, LongSupplier maxEnergyInput) {
        super(type, pos, state);
        this.maxEnergyInput = maxEnergyInput;
        this.energy = new EnergyComponent(energyCapacity, maxEnergyInput.getAsLong(), 0L, this::setChanged);
    }

    @Override
    public IEnergyStorage energyStorage(@Nullable Direction side) {
        return trackingStorage;
    }

    @Override
    public long networkDemandPerTick() {
        long storageRoom = Math.max(0, energy.getCapacity() - energy.getAmount());
        long remainingInput = Math.max(0, maxEnergyInput.getAsLong() - energyReceivedThisTick);
        return Math.min(remainingInput, storageRoom);
    }

    /** Reset the per-tick received counter; call at the top of the subclass tick. */
    protected void resetEnergyReceived() {
        energyReceivedThisTick = 0;
    }

    /** RF accepted into the buffer since the last {@link #resetEnergyReceived()}. */
    protected long energyReceivedThisTick() {
        return energyReceivedThisTick;
    }

    @Override
    protected void saveLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLogisticsData(tag, registries);
        energy.writeNbt(tag, "Energy");
    }

    @Override
    protected void loadLogisticsData(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLogisticsData(tag, registries);
        energy.readNbt(tag, "Energy");
    }
}
