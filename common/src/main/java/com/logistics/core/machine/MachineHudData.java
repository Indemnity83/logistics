package com.logistics.core.machine;

import com.logistics.core.lib.block.ProcessingMachine;
import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server-side capture of a processing machine's progress (and output tank, if any) into the tag a look-at
 * HUD (Jade) syncs to the client. Works off the {@link ProcessingMachine} / {@link HasFluidStorage}
 * contracts, so it covers the macerator, kiln, crucible, and any future machine without depending on a
 * specific domain. Read back by the client-side {@code MachineHudLines}.
 */
public final class MachineHudData {

    /** NBT key whose presence marks that the tag carries machine processing state. */
    public static final String KEY_PROCESSING = "processing";

    public static final String KEY_FLUID_ID = "fluidId";
    public static final String KEY_FLUID_AMOUNT = "fluidAmount";
    public static final String KEY_FLUID_CAPACITY = "fluidCapacity";

    private MachineHudData() {}

    public static void write(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof ProcessingMachine machine) {
            data.putBoolean(KEY_PROCESSING, machine.isProcessing());
            data.putFloat("progress", machine.processProgress());
        }
        if (blockEntity instanceof HasFluidStorage hasFluid) {
            IFluidStorage tank = hasFluid.fluidStorage(null);
            if (tank != null) {
                for (IFluidView view : tank.contents()) {
                    data.putInt(KEY_FLUID_ID, BuiltInRegistries.FLUID.getId(view.resource().getFluid()));
                    data.putInt(KEY_FLUID_AMOUNT, clampMb(view.amount()));
                    data.putInt(KEY_FLUID_CAPACITY, clampMb(view.capacity()));
                    break; // single-tank HUD
                }
            }
        }
    }

    private static int clampMb(long nativeAmount) {
        return (int) Math.min(FluidUnits.toMillibuckets(nativeAmount), Integer.MAX_VALUE);
    }
}
