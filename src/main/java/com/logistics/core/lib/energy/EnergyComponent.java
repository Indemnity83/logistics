package com.logistics.core.lib.energy;

import com.logistics.core.lib.storage.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Minimal energy component using Team Reborn Energy's {@link SimpleEnergyStorage}.
 * <p>
 * Handles NBT persistence and change notifications automatically.
 */
public final class EnergyComponent extends SimpleEnergyStorage {
    private final Runnable onChanged;

    public EnergyComponent(long capacity, long maxInsert, long maxExtract, Runnable onChanged) {
        super(capacity, maxInsert, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    protected void onFinalCommit() {
        onChanged.run();
    }

    public void readNbt(CompoundTag nbt, String key) {
        this.amount = NbtCompat.getLong(nbt, key, 0L);
    }

    public void writeNbt(CompoundTag nbt, String key) {
        nbt.putLong(key, this.amount);
    }
}