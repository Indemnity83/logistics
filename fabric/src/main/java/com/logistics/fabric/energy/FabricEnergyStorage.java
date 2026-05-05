package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

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
        if (simulate) {
            try (Transaction t = Transaction.openOuter()) {
                return super.insert(maxAmount, t);
                // t closes without commit → rollback
            }
        }
        try (Transaction t = Transaction.openOuter()) {
            long inserted = super.insert(maxAmount, t);
            t.commit();
            return inserted;
        }
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (simulate) {
            try (Transaction t = Transaction.openOuter()) {
                return super.extract(maxAmount, t);
            }
        }
        try (Transaction t = Transaction.openOuter()) {
            long extracted = super.extract(maxAmount, t);
            t.commit();
            return extracted;
        }
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
