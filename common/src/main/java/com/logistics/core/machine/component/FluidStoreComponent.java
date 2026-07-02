package com.logistics.core.machine.component;

import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.OutputOnlyFluidStorage;
import com.logistics.core.machine.MachineComponent;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Single-variant fluid tank component wrapping the {@link FluidTankComponent} holder.
 *
 * <p>When {@code outputOnly}, the externally exposed view ({@link #fluid}) rejects insertion so pipes can
 * only drain the tank — the machine fills it internally via {@link #tank()}.
 */
public final class FluidStoreComponent implements MachineComponent, MachineComponent.FluidAccess {

    private static final String LEGACY_KEY = "Tank";

    private final String id;
    private final FluidTankComponent tank;
    private final IFluidStorage exposed;

    public FluidStoreComponent(String id, long capacity, Runnable onChanged) {
        this(id, capacity, onChanged, false);
    }

    public FluidStoreComponent(String id, long capacity, Runnable onChanged, boolean outputOnly) {
        this.id = id;
        this.tank = new FluidTankComponent(capacity, onChanged);
        this.exposed = outputOnly ? new OutputOnlyFluidStorage(tank) : tank;
    }

    @Override
    public String id() {
        return id;
    }

    /** The underlying tank holder, for machine-specific work components. */
    public FluidTankComponent tank() {
        return tank;
    }

    @Override
    @Nullable
    public IFluidStorage fluid(@Nullable Direction side) {
        return exposed;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tank.writeNbt(tag, "fluid");
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readNbt(tag, "fluid");
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        tank.readNbt(root, LEGACY_KEY);
    }
}
