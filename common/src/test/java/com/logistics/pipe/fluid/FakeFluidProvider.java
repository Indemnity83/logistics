package com.logistics.pipe.fluid;

/** Test double: a finite provider of a single (String) fluid, measured in millibuckets. */
final class FakeFluidProvider implements FluidProvider<String> {

    private final String fluid;
    private long amountMb;

    FakeFluidProvider(String fluid, long amountMb) {
        this.fluid = fluid;
        this.amountMb = amountMb;
    }

    long amount() {
        return amountMb;
    }

    @Override
    public String fluid() {
        return amountMb > 0 ? fluid : null;
    }

    @Override
    public long drain(long millibuckets) {
        long drained = Math.max(0, Math.min(millibuckets, amountMb));
        amountMb -= drained;
        return drained;
    }
}
