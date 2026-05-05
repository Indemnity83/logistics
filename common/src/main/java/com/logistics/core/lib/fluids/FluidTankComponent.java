package com.logistics.core.lib.fluids;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.lib.storage.NbtCompat;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;
    }

    @Override
    protected void onFinalCommit() {
        onChanged.run();
    }

    public Storage<FluidVariant> storage() {
        return this;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    public void readNbt(CompoundTag nbt, String key) {
        CompoundTag data = NbtCompat.getCompoundOrEmpty(nbt, key);
        if (data.isEmpty() || !data.contains("fluid")) return;

        String id = NbtCompat.getString(data, "fluid", "");
        ResourceId resourceId = ResourceId.tryParse(id);
        Fluid fluid = Fluids.EMPTY;
        if (resourceId != null) {
            Fluid registryFluid = BuiltInRegistries.FLUID.getValue(resourceId.toIdentifier());
            if (registryFluid != null) {
                fluid = registryFluid;
            }
        }
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
            data.putLong("amount", amount);
        }
        if (!data.isEmpty()) {
            nbt.put(key, data);
        }
    }
}
