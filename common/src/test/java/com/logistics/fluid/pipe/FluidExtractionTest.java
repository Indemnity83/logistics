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
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("energy caps the transfer when scarce (50 mB per RF)")
    void energyCapsWhenScarce() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        // Rate well above what 5 RF can pay for, so energy is the binding limit: 5 RF × 50 mB/RF = 250 mB.
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 5, 1000, true, 0);
        assertThat(result.extractedMb()).isEqualTo(250);
        assertThat(result.energyToConsume()).isEqualTo(5);
        assertThat(pipe.amount()).isEqualTo(250);
    }

    @Test
    @DisplayName("no energy means no extraction when an engine is required")
    void noEnergyNoExtraction() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, sources(tank), 0, RATE, true, 0);
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
                FluidExtraction.tick(pipe, sources(new FakeFluidProvider(WATER, 1000)), 0, RATE, false, 0);
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
        FluidExtraction.Result result = FluidExtraction.tick(pipe, sources(a, b), 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(20); // 5 from a + 15 from b, not 1005
        assertThat(a.amount()).isZero();
        assertThat(b.amount()).isEqualTo(985);
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("charges one whole RF per MB_PER_RF of fluid moved")
    void chargesProportionalRf() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        // Rate equals exactly one RF's worth, so a full tick moves 50 mB and costs exactly 1 RF.
        FluidExtraction.Result result = FluidExtraction.tick(
                pipe, sources(new FakeFluidProvider(WATER, 1000)), 10, FluidExtraction.MB_PER_RF, true, 0);
        assertThat(result.extractedMb()).isEqualTo(FluidExtraction.MB_PER_RF);
        assertThat(result.energyToConsume()).isEqualTo(1);
        assertThat(result.carryMb()).isZero();
    }

    @Test
    @DisplayName("a sub-RF pull burns no energy yet but carries the remainder forward")
    void smallPullCarriesRemainder() {
        // First tick moves 20 mB (< 50), so no whole RF is due yet — it carries.
        FluidPipe<String> first = FluidPipe.extractor();
        FluidExtraction.Result a =
                FluidExtraction.tick(first, sources(new FakeFluidProvider(WATER, 1000)), 10, RATE, true, 0);
        assertThat(a.extractedMb()).isEqualTo(20);
        assertThat(a.energyToConsume()).isZero();
        assertThat(a.carryMb()).isEqualTo(20);

        // Feeding the carried remainder back in lets the next pull cross a whole RF: 40 carried + 20 = 60 → 1 RF, 10 left.
        FluidPipe<String> second = FluidPipe.extractor();
        FluidExtraction.Result b =
                FluidExtraction.tick(second, sources(new FakeFluidProvider(WATER, 1000)), 10, RATE, true, 40);
        assertThat(b.extractedMb()).isEqualTo(20);
        assertThat(b.energyToConsume()).isEqualTo(1);
        assertThat(b.carryMb()).isEqualTo(10);
    }

    @Test
    @DisplayName("burns no energy and preserves the carry when nothing is extracted")
    void burnsNothingWhenIdle() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FluidExtraction.Result result = FluidExtraction.tick(pipe, List.of(), 1000, RATE, true, 30); // no sources
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
        assertThat(result.carryMb()).isEqualTo(30); // carry untouched
    }

    @SafeVarargs
    private static List<FluidProvider<String>> sources(FluidProvider<String>... providers) {
        return List.of(providers);
    }
}
