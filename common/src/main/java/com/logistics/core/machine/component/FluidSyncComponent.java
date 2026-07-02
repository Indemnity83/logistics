package com.logistics.core.machine.component;

import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Pushes a block-entity client sync when the machine's tank content changes, so a block-face fluid
 * renderer stays current — {@code setChanged()} alone marks the chunk dirty but never reaches clients.
 * Checked a few times a second to bound the packet rate while a pipe drains the tank.
 */
public final class FluidSyncComponent implements MachineComponent {

    private static final long CHECK_INTERVAL_TICKS = 5L;

    private final FluidStoreComponent fluidStore;
    private long lastSyncedAmount = -1;
    private int lastSyncedFluidId = Integer.MIN_VALUE;

    public FluidSyncComponent(FluidStoreComponent fluidStore) {
        this.fluidStore = fluidStore;
    }

    @Override
    public String id() {
        return "fluid_sync";
    }

    @Override
    public void serverTick(MachineContext ctx) {
        if (ctx.level().getGameTime() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        FluidTankComponent tank = fluidStore.tank();
        long amount = tank.getAmount();
        int fluidId = tank.isEmpty()
                ? -1
                : BuiltInRegistries.FLUID.getId(tank.getFluidKey().getFluid());
        if (amount != lastSyncedAmount || fluidId != lastSyncedFluidId) {
            lastSyncedAmount = amount;
            lastSyncedFluidId = fluidId;
            ctx.sync();
        }
    }
}
