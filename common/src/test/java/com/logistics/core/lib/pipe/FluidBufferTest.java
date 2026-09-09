package com.logistics.core.lib.pipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidBuffer")
class FluidBufferTest {

    private static final String WATER = "water";
    private static final String LAVA = "lava";

    @Test
    @DisplayName("holds up to 250 mB internally")
    void capacityIs250Millibuckets() {
        FluidBuffer<String> pipe = new FluidBuffer<>();
        assertThat(pipe.capacity()).isEqualTo(250);
    }

    @Test
    @DisplayName("extracted fluid fills the pipe's internal storage")
    void extractedFluidFillsStorage() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        pipe.extract(ampleProvider(), 250);
        assertThat(pipe.amount()).isEqualTo(250);
    }

    @Test
    @DisplayName("extraction can't overfill the pipe")
    void extractionLimitedByCapacity() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        long extracted = pipe.extract(ampleProvider(), 1000); // ask for a full bucket into a 250 mB pipe
        assertThat(extracted).isEqualTo(250); // only what fits
        assertThat(pipe.amount()).isEqualTo(250); // pipe is full, not overfilled
    }

    @Test
    @DisplayName("fluid is removed from an adjacent provider equal to what is inserted into the pipe")
    void extractionDrainsProvider() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 500);
        long extracted = pipe.extract(tank, 250);
        assertThat(extracted).isEqualTo(250);
        assertThat(pipe.amount()).isEqualTo(250); // inserted into the pipe
        assertThat(tank.amount()).isEqualTo(250); // 500 - 250 drained from the provider
    }

    @Test
    @DisplayName("fluid transferred cannot exceed what the provider can provide")
    void extractionLimitedByProvider() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 100); // provider holds less than a full pipe
        long extracted = pipe.extract(tank, 250); // ask for a full pipe's worth
        assertThat(extracted).isEqualTo(100); // capped by the provider
        assertThat(pipe.amount()).isEqualTo(100); // only what the provider had
        assertThat(tank.amount()).isZero(); // provider fully drained
    }

    @Test
    @DisplayName("extraction is bounded by the requested amount")
    void extractionLimitedByRequestedAmount() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000); // neither capacity nor supply is the constraint
        long extracted = pipe.extract(tank, 20); // the caller's per-tick allowance
        assertThat(extracted).isEqualTo(20); // capped by the request
        assertThat(pipe.amount()).isEqualTo(20);
        assertThat(tank.amount()).isEqualTo(980);
    }

    @Test
    @DisplayName("fluid of one type cannot be transferred into a pipe holding another type")
    void rejectsMismatchedFluid() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        pipe.extract(new FakeFluidProvider(WATER, 100), 250); // pipe now holds 100 mB water, with room to spare

        FakeFluidProvider lava = new FakeFluidProvider(LAVA, 250);
        long extracted = pipe.extract(lava, 150); // try to top up with a different fluid
        assertThat(extracted).isZero(); // rejected: can't mix fluids
        assertThat(pipe.amount()).isEqualTo(100); // unchanged
        assertThat(pipe.fluid()).isEqualTo(WATER); // still water
        assertThat(lava.amount()).isEqualTo(250); // lava provider untouched
    }

    @Test
    @DisplayName("extraction of a non-positive amount is a no-op and never touches the provider")
    void rejectsNonPositiveExtraction() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 500);

        assertThat(pipe.extract(tank, 0)).isZero();
        assertThat(pipe.extract(tank, -50)).isZero();

        assertThat(pipe.amount()).isZero();
        assertThat(pipe.fluid()).isNull();
        assertThat(tank.amount()).isEqualTo(500); // provider untouched
    }

    @Test
    @DisplayName("a wider buffer fills to its own capacity, not the shared default")
    void honoursItsOwnCapacity() {
        FluidBuffer<String> pipe = FluidBuffer.extractor(1000);
        assertThat(pipe.capacity()).isEqualTo(1000);
        assertThat(pipe.space()).isEqualTo(1000);

        assertThat(pipe.extract(ampleProvider(), 1000)).isEqualTo(1000);
        assertThat(pipe.amount()).isEqualTo(1000);
        assertThat(pipe.space()).isZero();
        assertThat(pipe.extract(ampleProvider(), 1)).isZero();
    }

    /** A water provider with effectively unlimited fluid, for tests where the source isn't the constraint. */
    private static FakeFluidProvider ampleProvider() {
        return new FakeFluidProvider(WATER, 1_000_000);
    }
}
