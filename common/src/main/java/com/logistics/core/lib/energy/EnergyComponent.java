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
        long cap = getCapacity();
        long accepted = Math.min(maxAmount, Math.min(maxInsert, cap - amount));
        if (accepted > 0 && !simulate) {
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        long extracted = Math.min(maxAmount, Math.min(maxExtract, amount));
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
        return capacitySupplier.getAsLong();
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
        this.amount -= amount;
    }

    /** Sets the stored amount directly (e.g., client-side sync). Does not fire change notification. */
    public void setAmount(long amount) {
        this.amount = Math.max(0, Math.min(amount, getCapacity()));
    }

    public void readNbt(CompoundTag nbt, String key) {
        this.amount = Math.max(0, Math.min(NbtCompat.getLong(nbt, key, 0L), getCapacity()));
    }

    public void writeNbt(CompoundTag nbt, String key) {
        nbt.putLong(key, this.amount);
    }
}
