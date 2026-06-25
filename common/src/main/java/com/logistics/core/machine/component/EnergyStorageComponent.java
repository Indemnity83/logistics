package com.logistics.core.machine.component;

import com.logistics.core.lib.energy.EnergyComponent;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Energy buffer component wrapping the loader-agnostic {@link EnergyComponent} holder.
 *
 * <p>Optionally tracks energy received this tick and reports it as network demand, mirroring the
 * tracking-storage pattern that machines like the Macerator use today. Demand and received-tick
 * tracking require this component to tick before any work component that spends energy — which the
 * host guarantees by registration order.
 */
public final class EnergyStorageComponent implements MachineComponent, MachineComponent.EnergyAccess, MachineComponent.Demand {

    private static final String LEGACY_KEY = "Energy";

    private final String id;
    private final EnergyComponent storage;
    private final long capacity;
    private final long maxInsert;
    private final boolean trackReceived;
    private final boolean exposeDemand;

    private long receivedThisTick;

    private final IEnergyStorage tracking = new IEnergyStorage() {
        @Override
        public long insert(long maxAmount, boolean simulate) {
            long inserted = storage.insert(maxAmount, simulate);
            if (trackReceived && !simulate && inserted > 0) {
                receivedThisTick += inserted;
            }
            return inserted;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return storage.extract(maxAmount, simulate);
        }

        @Override
        public long getAmount() {
            return storage.getAmount();
        }

        @Override
        public long getCapacity() {
            return storage.getCapacity();
        }

        @Override
        public boolean canInsert() {
            return storage.canInsert();
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract();
        }
    };

    public EnergyStorageComponent(
            String id,
            long capacity,
            long maxInsert,
            long maxExtract,
            boolean trackReceived,
            boolean exposeDemand,
            Runnable onChanged) {
        this.id = id;
        this.storage = new EnergyComponent(capacity, maxInsert, maxExtract, onChanged);
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.trackReceived = trackReceived;
        this.exposeDemand = exposeDemand;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void serverTick(MachineContext ctx) {
        receivedThisTick = 0;
    }

    @Override
    @Nullable
    public IEnergyStorage energy(@Nullable Direction side) {
        return tracking;
    }

    @Override
    public long networkDemandPerTick() {
        if (!exposeDemand) {
            return 0;
        }
        long storageRoom = Math.max(0, capacity - storage.getAmount());
        long remainingInput = Math.max(0, maxInsert - receivedThisTick);
        return Math.min(remainingInput, storageRoom);
    }

    /** Current stored energy. */
    public long amount() {
        return storage.getAmount();
    }

    /** Deducts energy for internal work (no change notification, matching the holder's semantics). */
    public void consume(long rf) {
        storage.consume(rf);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        storage.writeNbt(tag, "amount");
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        storage.readNbt(tag, "amount");
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        storage.readNbt(root, LEGACY_KEY);
    }
}
