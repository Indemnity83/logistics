package com.logistics.core.lib.pipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidExtraction.tick")
class FluidExtractionTest {

    private static final String WATER = "water";
    private static final long RATE = 20;

    @Test
    @DisplayName("transfers up to the rate when energy is plentiful")
    void rateCapsWhenEnergyPlentiful() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, new FakeFluidProvider(WATER, 1000), 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("energy caps the transfer when scarce (50 mB per RF)")
    void energyCapsWhenScarce() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        // Rate well above what 5 RF can pay for, so energy is the binding limit: 5 RF × 50 mB/RF = 250 mB.
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, new FakeFluidProvider(WATER, 1000), 5, 1000, true, 0);
        assertThat(result.extractedMb()).isEqualTo(250);
        assertThat(result.energyToConsume()).isEqualTo(5);
        assertThat(pipe.amount()).isEqualTo(250);
    }

    @Test
    @DisplayName("no energy means no extraction when an engine is required")
    void noEnergyNoExtraction() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, tank, 0, RATE, true, 0);
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
        assertThat(pipe.amount()).isZero();
        assertThat(tank.amount()).isEqualTo(1000); // provider untouched
    }

    @Test
    @DisplayName("free mode transfers at the rate ignoring energy")
    void freeModeIgnoresEnergy() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FluidExtraction.Result result =
                FluidExtraction.tick(pipe, new FakeFluidProvider(WATER, 1000), 0, RATE, false, 0);
        assertThat(result.extractedMb()).isEqualTo(20);
        assertThat(result.energyToConsume()).isZero(); // free mode never burns energy
        assertThat(pipe.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("a partially-filled source caps the transfer at what it holds")
    void sourceCapsTransfer() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeFluidProvider source = new FakeFluidProvider(WATER, 5); // less than the rate
        FluidExtraction.Result result = FluidExtraction.tick(pipe, source, 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(5);
        assertThat(source.amount()).isZero();
        assertThat(pipe.amount()).isEqualTo(5);
    }

    @Test
    @DisplayName("charges one whole RF per MB_PER_RF of fluid moved")
    void chargesProportionalRf() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        // Rate equals exactly one RF's worth, so a full tick moves 50 mB and costs exactly 1 RF.
        FluidExtraction.Result result = FluidExtraction.tick(
                pipe, new FakeFluidProvider(WATER, 1000), 10, FluidExtraction.MB_PER_RF, true, 0);
        assertThat(result.extractedMb()).isEqualTo(FluidExtraction.MB_PER_RF);
        assertThat(result.energyToConsume()).isEqualTo(1);
        assertThat(result.carryMb()).isZero();
    }

    @Test
    @DisplayName("a sub-RF pull burns no energy yet but carries the remainder forward")
    void smallPullCarriesRemainder() {
        // First tick moves 20 mB (< 50), so no whole RF is due yet — it carries.
        FluidBuffer<String> first = FluidBuffer.extractor();
        FluidExtraction.Result a =
                FluidExtraction.tick(first, new FakeFluidProvider(WATER, 1000), 10, RATE, true, 0);
        assertThat(a.extractedMb()).isEqualTo(20);
        assertThat(a.energyToConsume()).isZero();
        assertThat(a.carryMb()).isEqualTo(20);

        // Feeding the carried remainder back in lets the next pull cross a whole RF: 40 carried + 20 = 60 → 1 RF, 10 left.
        FluidBuffer<String> second = FluidBuffer.extractor();
        FluidExtraction.Result b =
                FluidExtraction.tick(second, new FakeFluidProvider(WATER, 1000), 10, RATE, true, 40);
        assertThat(b.extractedMb()).isEqualTo(20);
        assertThat(b.energyToConsume()).isEqualTo(1);
        assertThat(b.carryMb()).isEqualTo(10);
    }

    @Test
    @DisplayName("burns no energy and preserves the carry when nothing is extracted")
    void burnsNothingWhenIdle() {
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FluidExtraction.Result result = FluidExtraction.tick(pipe, new FakeFluidProvider(WATER, 0), 1000, RATE, true, 30);
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
        assertThat(result.carryMb()).isEqualTo(30); // carry untouched
    }

    @Test
    @DisplayName("re-offers the whole buffer when an all-or-nothing source refuses a rate-sized sip")
    void gulpsFromAllOrNothingSource() {
        // A lava cauldron parts with one whole bucket or nothing; the 20 mB rate alone would never move any.
        FluidBuffer<String> pipe = FluidBuffer.extractor(1000);
        FakeQuantizedFluidProvider cauldron = new FakeQuantizedFluidProvider(WATER, 1000, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, cauldron, 20, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(1000);
        assertThat(result.energyToConsume()).isEqualTo(20); // 1000 mB ÷ 50 mB/RF
        assertThat(cauldron.amount()).isZero();
        assertThat(pipe.amount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("gulping still obeys the energy cap")
    void gulpRespectsEnergyCap() {
        // 10 RF buys 500 mB — not enough for a 1000 mB chunk, so nothing moves and nothing is spent.
        FluidBuffer<String> pipe = FluidBuffer.extractor(1000);
        FakeQuantizedFluidProvider cauldron = new FakeQuantizedFluidProvider(WATER, 1000, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, cauldron, 10, RATE, true, 0);
        assertThat(result.extractedMb()).isZero();
        assertThat(result.energyToConsume()).isZero();
        assertThat(cauldron.amount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("gulping is bounded by the buffer, so a chunk larger than the pipe never moves")
    void gulpBoundedByBuffer() {
        // The default 250 mB pipe cannot hold a 333 mB cauldron level, so it stays put rather than overfilling.
        FluidBuffer<String> pipe = FluidBuffer.extractor();
        FakeQuantizedFluidProvider cauldron = new FakeQuantizedFluidProvider(WATER, 333, 999);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, cauldron, 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isZero();
        assertThat(cauldron.amount()).isEqualTo(999);
        assertThat(pipe.amount()).isZero();
    }

    @Test
    @DisplayName("a divisible source is still drained at the rate, not gulped")
    void divisibleSourceStillRateLimited() {
        FluidBuffer<String> pipe = FluidBuffer.extractor(1000);
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000);
        FluidExtraction.Result result = FluidExtraction.tick(pipe, tank, 1000, RATE, true, 0);
        assertThat(result.extractedMb()).isEqualTo(RATE);
        assertThat(tank.amount()).isEqualTo(980);
    }
}
