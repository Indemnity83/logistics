package com.logistics.core.lib.fluids;

/**
 * An {@link IFluidStorage} view that rejects insertion but forwards extraction, for exposing a machine's
 * output tank to pipes: the machine fills the tank internally, while external handlers can only drain it.
 */
public final class OutputOnlyFluidStorage implements IFluidStorage {

    private final IFluidStorage backing;

    public OutputOnlyFluidStorage(IFluidStorage backing) {
        this.backing = backing;
    }

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        return 0;
    }

    @Override
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        return backing.extract(fluid, maxAmount, simulate);
    }

    @Override
    public Iterable<IFluidView> contents() {
        return backing.contents();
    }
}
