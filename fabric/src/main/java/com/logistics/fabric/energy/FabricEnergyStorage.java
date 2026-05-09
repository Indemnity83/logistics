package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Dual-interface energy storage: implements both {@link IEnergyStorage} (loader-agnostic)
 * and extends Team Reborn's {@link SimpleEnergyStorage} (Fabric-native).
 *
 * <p>Translates {@link IEnergyStorage}'s simulate-boolean API into Team Reborn's
 * transaction system. When {@code simulate=true}, opens a transaction that rolls back
 * on close; when {@code simulate=false}, opens and commits a transaction.
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
        // Use direct arithmetic — safe to call from any context, including within a TR
        // transaction's close callback or from the cable network's simulate-boolean path.
        // The TR insert(long, TransactionContext) method is still available for callers
        // that hold a transaction (e.g. the energy push service in LogisticsFabric).
        long toInsert = Math.min(maxAmount, Math.min(maxInsert, Math.max(0, capacity - amount)));
        if (!simulate) {
            amount += toInsert;
        }
        return toInsert;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        long toExtract = Math.min(maxAmount, Math.min(maxExtract, amount));
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
