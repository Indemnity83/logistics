package com.logistics.fluid.pipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidExtraction.tick")
class FluidExtractionTest {

    private static final String WATER = "water";
    private static final long RATE = 20;

    @Test
    @DisplayName("transfers up to the rate when energy is plentiful")
    void rateCapsWhenEnergyPlentiful() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 1000, RATE, true);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("energy caps the transfer when scarce (1 RF = 1 mB)")
    void energyCapsWhenScarce() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 5, RATE, true);
        assertThat(result.extractedMb()).isEqualTo(5);
        assertThat(pipe.amount()).isEqualTo(5);
    }

    @Test
    @DisplayName("no energy means no extraction when an engine is required")
    void noEnergyNoExtraction() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, sources(tank), 0, RATE, true);
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
        assertThat(pipe.amount()).isZero();
        assertThat(tank.amount()).isEqualTo(1000); // provider untouched
    }

    @Test
    @DisplayName("free mode transfers at the rate ignoring energy")
    void freeModeIgnoresEnergy() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 0, RATE, false);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(result.energyToConsume()).isZero(); // free mode never burns energy
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("total transfer across multiple sources is capped at the rate")
    void multiSourceTotalCappedAtRate() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider a = new FakeFluidProvider(WATER, 5); // partial
        FakeFluidProvider b = new FakeFluidProvider(WATER, 1000); // plenty
        FluidExtraction.Result result = FluidExtraction.tick(pipe, sources(a, b), 1000, RATE, true);
        assertThat(result.extractedMb()).isEqualTo(20); // 5 from a + 15 from b, not 1005
        assertThat(a.amount()).isZero();
        assertThat(b.amount()).isEqualTo(985);
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("burns the whole energy buffer when anything is extracted (no coasting)")
    void burnsWholeBufferOnExtraction() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 1000, RATE, true);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(result.energyToConsume()).isEqualTo(1000); // flush the buffer, not just the 20 used
    }

    @Test
    @DisplayName("burns no energy when nothing is extracted")
    void burnsNothingWhenIdle() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result = FluidExtraction.tick(pipe, List.of(), 1000, RATE, true); // no sources
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
    }

    @SafeVarargs
    private static List<FluidProvider<String>> sources(FluidProvider<String>... providers) {
        return List.of(providers);
    }
}
