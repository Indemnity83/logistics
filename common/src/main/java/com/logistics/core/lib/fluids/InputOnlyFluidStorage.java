package com.logistics.core.lib.fluids;

import java.util.function.Predicate;

/**
 * An {@link IFluidStorage} view that accepts only fluids matching a filter and refuses all extraction,
 * for exposing a machine's input tank to pipes: external handlers can fill it, while the machine drains
 * it internally.
 */
public final class InputOnlyFluidStorage implements IFluidStorage {

    private final IFluidStorage backing;
    private final Predicate<IFluidKey> filter;

    public InputOnlyFluidStorage(IFluidStorage backing, Predicate<IFluidKey> filter) {
        this.backing = backing;
        this.filter = filter;
    }

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        if (maxAmount <= 0 || fluid.isBlank() || !filter.test(fluid)) {
            return 0;
        }
        return backing.insert(fluid, maxAmount, simulate);
    }

    @Override
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        return 0;
    }

    @Override
    public Iterable<IFluidView> contents() {
        return backing.contents();
    }
}
