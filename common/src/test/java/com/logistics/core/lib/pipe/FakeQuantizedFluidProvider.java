package com.logistics.core.lib.pipe;

/**
 * Test double: an all-or-nothing provider that only parts with whole {@code quantumMb} chunks, the way a
 * cauldron yields whole levels. A drain request smaller than one chunk yields nothing at all.
 */
final class FakeQuantizedFluidProvider implements FluidProvider<String> {

    private final String fluid;
    private final long quantumMb;
    private long amountMb;

    FakeQuantizedFluidProvider(String fluid, long quantumMb, long amountMb) {
        this.fluid = fluid;
        this.quantumMb = quantumMb;
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
        long chunks = Math.min(millibuckets / quantumMb, amountMb / quantumMb);
        long drained = Math.max(0, chunks) * quantumMb;
        amountMb -= drained;
        return drained;
    }
}
