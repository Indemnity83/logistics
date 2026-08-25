package com.logistics.core.lib.fluids;

import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.core.lib.resource.ResourceId;
import java.util.Collections;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Simple, loader-agnostic fluid tank component implementing {@link IFluidStorage}.
 *
 * <p>Stores a single fluid type at a time (single-variant semantics). Uses vanilla Minecraft
 * types ({@link Fluid}, {@link DataComponentPatch}) for state storage — no loader-specific
 * dependencies. Loader adapters (e.g. {@code FabricFluidStorage}) bridge this to the platform's
 * native fluid transfer API.
 *
 * <p>Example usage:
 * <pre>{@code
 * private final FluidTankComponent tank = new FluidTankComponent(4000, this::markDirtyAndSync);
 * }</pre>
 */
public class FluidTankComponent implements IFluidStorage {

    private final long capacity;
    private final Runnable onChanged;

    private Fluid fluid = Fluids.EMPTY;
    private DataComponentPatch components = DataComponentPatch.EMPTY;
    private long amount = 0;

    public FluidTankComponent(long capacity, Runnable onChanged) {
        this.capacity = capacity;
        this.onChanged = onChanged;
    }

    // ==================== IFluidStorage ====================

    @Override
    public long insert(IFluidKey key, long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || key.isBlank()) return 0;

        boolean isEmpty = fluid == Fluids.EMPTY;
        if (!isEmpty && !matchesStored(key)) return 0;

        long accepted = Math.min(maxAmount, capacity - amount);
        if (accepted <= 0) return 0;

        if (!simulate) {
            if (isEmpty) {
                fluid = key.getFluid();
                components = key.getComponents();
            }
            amount += accepted;
            onChanged.run();
        }
        return accepted;
    }

    @Override
    public long extract(IFluidKey key, long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || key.isBlank()) return 0;
        if (!matchesStored(key)) return 0;

        long extracted = Math.min(maxAmount, amount);
        if (extracted <= 0) return 0;

        if (!simulate) {
            amount -= extracted;
            if (amount == 0) {
                fluid = Fluids.EMPTY;
                components = DataComponentPatch.EMPTY;
            }
            onChanged.run();
        }
        return extracted;
    }

    @Override
    public Iterable<IFluidView> contents() {
        if (fluid == Fluids.EMPTY || amount <= 0) return Collections.emptyList();
        IFluidKey key = currentKey();
        long currentAmount = amount;
        long currentCapacity = capacity;
        return Collections.singletonList(new IFluidView() {
            @Override public IFluidKey resource() { return key; }
            @Override public long amount() { return currentAmount; }
            @Override public long capacity() { return currentCapacity; }
        });
    }

    // ==================== Accessors ====================

    /** Returns an {@link IFluidKey} representing the currently stored fluid, or a blank key if empty. */
    public IFluidKey getFluidKey() {
        return currentKey();
    }

    /**
     * Raw write of the contents, bypassing fluid-match/capacity checks (used by tank-column settling).
     * An {@code amount} of 0 or a blank key empties the tank; otherwise the amount is clamped to capacity.
     */
    public void setContents(IFluidKey key, long amount) {
        if (key == null || key.isBlank() || amount <= 0) {
            fluid = Fluids.EMPTY;
            components = DataComponentPatch.EMPTY;
            this.amount = 0;
        } else {
            fluid = key.getFluid();
            components = key.getComponents();
            this.amount = Math.min(amount, capacity);
        }
        onChanged.run();
    }

    /** Returns the current amount of fluid stored, in platform-native units. */
    public long getAmount() {
        return amount;
    }

    /** Returns the maximum capacity, in platform-native units. */
    public long getCapacity() {
        return capacity;
    }

    /** Returns {@code true} if no fluid is stored. */
    public boolean isEmpty() {
        return fluid == Fluids.EMPTY || amount <= 0;
    }

    // ==================== NBT Persistence ====================

    public void readNbt(CompoundTag nbt, String key) {
        CompoundTag data = NbtCompat.getCompoundOrEmpty(nbt, key);
        if (data.isEmpty() || !data.contains("fluid")) {
            // No fluid data means the tank is empty. Reset, rather than retaining stale contents — this
            // is how an emptied tank/pipe syncs to the client (writeNbt records no fluid field when empty).
            fluid = Fluids.EMPTY;
            components = DataComponentPatch.EMPTY;
            amount = 0;
            return;
        }

        String id = NbtCompat.getString(data, "fluid", "");
        ResourceId resourceId = ResourceId.tryParse(id);
        fluid = Fluids.EMPTY;
        if (resourceId != null) {
            Fluid registryFluid = BuiltInRegistries.FLUID.getValue(resourceId.toIdentifier());
            if (registryFluid != null) {
                fluid = registryFluid;
            }
        }
        CompoundTag compTag = NbtCompat.getCompoundOrEmpty(data, "components");
        components = compTag.isEmpty() ? DataComponentPatch.EMPTY :
                DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, compTag).result().orElse(DataComponentPatch.EMPTY);

        // Amounts are clamped to capacity; unresolved fluids produce an empty tank.
        long savedAmount = NbtCompat.getLong(data, "amount", 0L);
        amount = fluid == Fluids.EMPTY ? 0L : Math.min(Math.max(savedAmount, 0L), capacity);
        if (amount <= 0) {
            fluid = Fluids.EMPTY;
            components = DataComponentPatch.EMPTY;
            amount = 0;
        }
    }

    public void writeNbt(CompoundTag nbt, String key) {
        // Always emit the key — including when empty — so an emptied tank still produces non-empty
        // block-entity data. Otherwise the owning BlockEntity omits its data tag, the client's load
        // path skips deserialization, and the stale (non-empty) contents are never cleared on screen.
        CompoundTag data = new CompoundTag();
        data.putLong("amount", fluid == Fluids.EMPTY ? 0L : amount);
        if (fluid != Fluids.EMPTY) {
            data.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            if (!components.isEmpty()) {
                DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, components)
                        .result().ifPresent(tag -> data.put("components", tag));
            }
        }
        nbt.put(key, data);
    }

    // ==================== Internals ====================

    /** Whether {@code key} identifies the fluid currently stored (same fluid and components). */
    private boolean matchesStored(IFluidKey key) {
        return fluid == key.getFluid() && components.equals(key.getComponents());
    }

    private IFluidKey currentKey() {
        Fluid f = fluid;
        DataComponentPatch c = components;
        return new IFluidKey() {
            @Override public Fluid getFluid() { return f; }
            @Override public DataComponentPatch getComponents() { return c; }
            @Override public boolean equals(Object o) {
                if (!(o instanceof IFluidKey other)) return false;
                return f == other.getFluid() && c.equals(other.getComponents());
            }
            @Override public int hashCode() { return 31 * f.hashCode() + c.hashCode(); }
        };
    }
}
