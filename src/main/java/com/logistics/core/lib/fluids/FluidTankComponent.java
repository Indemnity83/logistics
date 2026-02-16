package com.logistics.core.lib.fluids;

import com.logistics.core.lib.storage.NbtCompat;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

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
        CompoundTag data = NbtCompat.getCompoundOrEmpty(nbt, key);
        if (data.isEmpty()) return;

        String id = NbtCompat.getString(data, "fluid", "");
        Fluid fluid = id.isEmpty() ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
        CompoundTag compTag = NbtCompat.getCompoundOrEmpty(data, "components");
        DataComponentPatch comp = compTag.isEmpty() ? DataComponentPatch.EMPTY :
                DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, compTag).result().orElse(DataComponentPatch.EMPTY);

        variant = FluidVariant.of(fluid, comp);
        amount = NbtCompat.getLong(data, "amount", 0L);
    }

    public void writeNbt(CompoundTag nbt, String key) {
        CompoundTag data = new CompoundTag();
        if (variant.getFluid() != Fluids.EMPTY) {
            data.putString("fluid", BuiltInRegistries.FLUID.getKey(variant.getFluid()).toString());
            if (!variant.getComponents().isEmpty()) {
                DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, variant.getComponents())
                        .result().ifPresent(tag -> data.put("components", tag));
            }
        }
        data.putLong("amount", amount);
        nbt.put(key, data);
    }
}