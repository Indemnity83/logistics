package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges the NeoForge 21.1 {@link net.neoforged.neoforge.energy.IEnergyStorage} to the
 * common {@link IEnergyStorage} abstraction and vice versa.
 *
 * <p>NeoForge 21.1 uses a simulate-boolean API with no transaction system, which maps
 * cleanly to the common interface's simulate parameter. No journaling or snapshots are needed.
 */
public final class NeoForgeEnergyStorage implements IEnergyStorage {
    private final net.neoforged.neoforge.energy.IEnergyStorage handler;

    private NeoForgeEnergyStorage(net.neoforged.neoforge.energy.IEnergyStorage handler) {
        this.handler = handler;
    }

    /**
     * Wrap a NeoForge {@link net.neoforged.neoforge.energy.IEnergyStorage} as the common
     * {@link IEnergyStorage}.
     *
     * @param handler the NeoForge handler to wrap; may be {@code null}
     * @return the wrapped storage, or {@code null} if handler is {@code null}
     */
    @Nullable
    public static IEnergyStorage wrap(@Nullable net.neoforged.neoforge.energy.IEnergyStorage handler) {
        return handler == null ? null : new NeoForgeEnergyStorage(handler);
    }

    /**
     * Adapt a common {@link IEnergyStorage} to a NeoForge
     * {@link net.neoforged.neoforge.energy.IEnergyStorage}.
     *
     * @param storage the common storage to adapt; may be {@code null}
     * @return the NeoForge adapter, or {@code null} if storage is {@code null}
     */
    @Nullable
    public static net.neoforged.neoforge.energy.IEnergyStorage asNeoForge(@Nullable IEnergyStorage storage) {
        return storage == null ? null : new CommonEnergyHandler(storage);
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        if (!handler.canReceive() || maxAmount <= 0) {
            return 0;
        }
        return handler.receiveEnergy(clampToInt(maxAmount), simulate);
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (!handler.canExtract() || maxAmount <= 0) {
            return 0;
        }
        return handler.extractEnergy(clampToInt(maxAmount), simulate);
    }

    @Override
    public long getAmount() {
        return handler.getEnergyStored();
    }

    @Override
    public long getCapacity() {
        return handler.getMaxEnergyStored();
    }

    @Override
    public boolean canInsert() {
        return handler.canReceive();
    }

    @Override
    public boolean canExtract() {
        return handler.canExtract();
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    /**
     * Adapts a common {@link IEnergyStorage} to NeoForge's
     * {@link net.neoforged.neoforge.energy.IEnergyStorage}.
     *
     * <p>The simulate boolean maps directly — no transactions or snapshots are involved.
     */
    private static final class CommonEnergyHandler implements net.neoforged.neoforge.energy.IEnergyStorage {
        private final IEnergyStorage storage;

        private CommonEnergyHandler(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            return clampToInt(storage.insert(maxReceive, simulate));
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            return clampToInt(storage.extract(maxExtract, simulate));
        }

        @Override
        public int getEnergyStored() {
            return clampToInt(storage.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return clampToInt(storage.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public boolean canReceive() {
            return storage.canInsert();
        }
    }
}
