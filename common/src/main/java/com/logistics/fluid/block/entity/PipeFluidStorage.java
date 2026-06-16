package com.logistics.fluid.block.entity;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import com.logistics.pipe.fluid.FluidUnits;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;

/**
 * The fluid capability a pipe exposes on one side. Like the item pipes' {@code PipeItemStorage}, it is
 * <b>input only</b>: external pushers (a pump, a machine output) insert fluid, which becomes a traveling
 * parcel; you can't pull fluid back out of a pipe through the capability ({@link #extract} returns 0).
 * {@link #contents} reports the pipe's single fluid + total so machines/Jade can read what's flowing.
 *
 * <p>All capability amounts are platform-native; the pipe works in millibuckets, so this adapts at the
 * boundary via {@link FluidUnits}.
 */
public final class PipeFluidStorage implements IFluidStorage {

    private final FluidPipeBlockEntity pipe;
    private final Direction side;

    public PipeFluidStorage(FluidPipeBlockEntity pipe, Direction side) {
        this.pipe = pipe;
        this.side = side;
    }

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        long requestMb = FluidUnits.toMillibuckets(maxAmount);
        if (requestMb <= 0) {
            return 0;
        }
        long accepted = Math.min(requestMb, pipe.roomFor(fluid, side));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            pipe.acceptFluid(fluid, accepted, side);
        }
        return FluidUnits.mb(accepted);
    }

    @Override
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        return 0; // pipes are input-only; fluid leaves only by hopping along, never by being pulled out
    }

    @Override
    public Iterable<IFluidView> contents() {
        IFluidKey fluid = pipe.containedFluid();
        long total = pipe.totalMillibuckets();
        if (fluid.isBlank() || total <= 0) {
            return Collections.emptyList();
        }
        long nativeAmount = FluidUnits.mb(total);
        return List.of(new IFluidView() {
            @Override
            public IFluidKey resource() {
                return fluid;
            }

            @Override
            public long amount() {
                return nativeAmount;
            }
        });
    }
}
