package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Dual-interface energy storage: implements both {@link IEnergyStorage} (loader-agnostic)
 * and extends Team Reborn's {@link SimpleEnergyStorage} (Fabric-native).
 *
 * <p>The simulate-boolean {@link IEnergyStorage} methods use direct arithmetic — no TR
 * transactions are opened. When {@code simulate=true} the state is unchanged; when
 * {@code simulate=false} the amount is mutated in place.
 *
 * <p>Because this class IS already a Team Reborn {@code EnergyStorage}, the Fabric
 * capability lookup can hand it directly to TRE's {@code SIDED} system — no further
 * wrapping needed.
 */
public class FabricEnergyStorage extends SimpleEnergyStorage implements IEnergyStorage {

    public FabricEnergyStorage(long capacity, long maxInsert, long maxExtract) {
        super(capacity, maxInsert, maxExtract);
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        // Use direct arithmetic — safe to call from any context, including within a TR
        // transaction's close callback or from the cable network's simulate-boolean path.
        // The TR insert(long, TransactionContext) method is still available for callers
        // that hold a transaction (e.g. the energy push service in LogisticsFabric).
        long room = Math.max(0, Math.max(0, capacity) - Math.max(0, amount));
        long toInsert = Math.min(maxAmount, Math.min(Math.max(0, maxInsert), room));
        if (!simulate) {
            amount += toInsert;
        }
        return toInsert;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        long toExtract = Math.min(maxAmount, Math.min(Math.max(0, maxExtract), Math.max(0, amount)));
        if (!simulate) {
            amount -= toExtract;
        }
        return toExtract;
    }

    @Override
    public long getAmount() {
        return super.getAmount();
    }

    @Override
    public long getCapacity() {
        return super.getCapacity();
    }

    @Override
    public boolean canInsert() {
        return maxInsert > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
}
