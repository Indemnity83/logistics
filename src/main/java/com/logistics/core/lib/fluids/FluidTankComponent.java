package com.logistics.core.lib.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.nbt.CompoundTag;

/**
 * Minimal fluid tank component using the Transfer API's {@link SingleVariantStorage}.
 * <p>
 * This is an abstract class because {@link SingleVariantStorage} requires it.
 * Use it like this:
 * <pre>{@code
 * private final FluidTankComponent tank =
 *     new FluidTankComponent(4000, this::markDirtyAndSync) {};
 * }</pre>
 */
public abstract class FluidTankComponent extends SingleVariantStorage<FluidVariant> {
    private final long capacity;
    private final Runnable onChanged;

    protected FluidTankComponent(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;
    }

    @Override
    protected void onFinalCommit() {
        onChanged.run();
    }

    public void readNbt(CompoundTag nbt, String key) {
        if (nbt.contains(key)) {
            fromNbt(nbt.getCompound(key));
        }
    }

    public void writeNbt(CompoundTag nbt, String key) {
        nbt.put(key, toNbt());
    }
}