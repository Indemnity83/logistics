package com.logistics.core.lib.energy;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.function.LongSupplier;
import net.minecraft.nbt.CompoundTag;

/**
 * Simple, loader-agnostic energy storage component.
 *
 * <p>Implements {@link IEnergyStorage} without any platform-specific dependencies.
 * Handles NBT persistence and change notifications automatically.
 *
 * <p>Use {@link #consume(long)} for internal energy deductions (no change notification).
 * Use {@link #setAmount(long)} for direct assignment (e.g. client-side sync).
 */
public final class EnergyComponent implements IEnergyStorage {
    private long amount;
    private final LongSupplier capacitySupplier;
    private final long maxInsert;
    private final long maxExtract;
    private final Runnable onChanged;

    public EnergyComponent(long capacity, long maxInsert, long maxExtract, Runnable onChanged) {
        this(() -> capacity, maxInsert, maxExtract, onChanged);
    }

    /** Variant for dynamic capacity (e.g. engines whose buffer size is determined by subclass). */
    public EnergyComponent(LongSupplier capacitySupplier, long maxInsert, long maxExtract, Runnable onChanged) {
        this.capacitySupplier = capacitySupplier;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.onChanged = onChanged;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        long cap = getCapacity();
        long room = Math.max(0, cap - amount);
        long accepted = Math.min(maxAmount, Math.min(Math.max(0, maxInsert), room));
        if (accepted > 0 && !simulate) {
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        long extracted = Math.min(maxAmount, Math.min(Math.max(0, maxExtract), Math.max(0, amount)));
        if (extracted > 0 && !simulate) {
            amount -= extracted;
            onChanged.run();
        }
        return extracted;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return Math.max(0, capacitySupplier.getAsLong());
    }

    @Override
    public boolean canInsert() {
        return maxInsert > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    /** Consumes energy internally (e.g., machine processing). Does not fire change notification. */
    public void consume(long amount) {
        if (amount <= 0) return;
        this.amount = Math.max(0, this.amount - amount);
    }

    /** Sets the stored amount directly (e.g., client-side sync). Does not fire change notification. */
    public void setAmount(long amount) {
        this.amount = clampToCapacity(amount);
    }

    public void readNbt(CompoundTag nbt, String key) {
        this.amount = clampToCapacity(NbtCompat.getLong(nbt, key, 0L));
    }

    private long clampToCapacity(long value) {
        return Math.max(0, Math.min(value, getCapacity()));
    }

    public void writeNbt(CompoundTag nbt, String key) {
        nbt.putLong(key, this.amount);
    }
}
