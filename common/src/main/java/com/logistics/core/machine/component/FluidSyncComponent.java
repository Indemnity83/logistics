package com.logistics.core.machine.component;

import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.machine.MachineComponent;
import com.logistics.core.machine.MachineContext;
import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Pushes a block-entity client sync when any watched tank's content changes, so a block-face fluid
 * renderer stays current — {@code setChanged()} alone marks the chunk dirty but never reaches clients.
 * Checked a few times a second to bound the packet rate while a pipe drains a tank. A machine with more
 * than one tank (e.g. the refinery's input + output) passes them all to a single instance.
 */
public final class FluidSyncComponent implements MachineComponent {

    private static final long CHECK_INTERVAL_TICKS = 5L;

    private final FluidStoreComponent[] stores;
    private final long[] lastSyncedAmount;
    private final int[] lastSyncedFluidId;

    public FluidSyncComponent(FluidStoreComponent... stores) {
        this.stores = stores;
        this.lastSyncedAmount = new long[stores.length];
        this.lastSyncedFluidId = new int[stores.length];
        Arrays.fill(lastSyncedAmount, -1);
        Arrays.fill(lastSyncedFluidId, Integer.MIN_VALUE);
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
        boolean changed = false;
        for (int i = 0; i < stores.length; i++) {
            FluidTankComponent tank = stores[i].tank();
            long amount = tank.getAmount();
            int fluidId = tank.isEmpty()
                    ? -1
                    : BuiltInRegistries.FLUID.getId(tank.getFluidKey().getFluid());
            if (amount != lastSyncedAmount[i] || fluidId != lastSyncedFluidId[i]) {
                lastSyncedAmount[i] = amount;
                lastSyncedFluidId[i] = fluidId;
                changed = true;
            }
        }
        if (changed) {
            ctx.sync();
        }
    }
}
