package com.logistics.fluid.pipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FluidPipe")
class FluidPipeTest {

    private static final String WATER = "water";
    private static final String LAVA = "lava";

    @Test
    @DisplayName("holds up to 250 mB internally")
    void capacityIs250Millibuckets() {
        FluidPipe<String> pipe = new FluidPipe<>();
        assertThat(pipe.capacity()).isEqualTo(250);
    }

    @Test
    @DisplayName("extracted fluid fills the pipe's internal storage")
    void extractedFluidFillsStorage() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        pipe.extract(ampleProvider(), 250);
        assertThat(pipe.amount()).isEqualTo(250);
    }

    @Test
    @DisplayName("extraction can't overfill the pipe")
    void extractionLimitedByCapacity() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        long extracted = pipe.extract(ampleProvider(), 1000); // ask for a full bucket into a 250 mB pipe
        assertThat(extracted).isEqualTo(250); // only what fits
        assertThat(pipe.amount()).isEqualTo(250); // pipe is full, not overfilled
    }

    @Test
    @DisplayName("fluid is removed from an adjacent provider equal to what is inserted into the pipe")
    void extractionDrainsProvider() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 500);
        long extracted = pipe.extract(tank, 250);
        assertThat(extracted).isEqualTo(250);
        assertThat(pipe.amount()).isEqualTo(250); // inserted into the pipe
        assertThat(tank.amount()).isEqualTo(250); // 500 - 250 drained from the provider
    }

    @Test
    @DisplayName("fluid transferred cannot exceed what the provider can provide")
    void extractionLimitedByProvider() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 100); // provider holds less than a full pipe
        long extracted = pipe.extract(tank, 250); // ask for a full pipe's worth
        assertThat(extracted).isEqualTo(100); // capped by the provider
        assertThat(pipe.amount()).isEqualTo(100); // only what the provider had
        assertThat(tank.amount()).isZero(); // provider fully drained
    }

    @Test
    @DisplayName("extraction is bounded by the requested amount")
    void extractionLimitedByRequestedAmount() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 1000); // neither capacity nor supply is the constraint
        long extracted = pipe.extract(tank, 20); // the caller's per-tick allowance
        assertThat(extracted).isEqualTo(20); // capped by the request
        assertThat(pipe.amount()).isEqualTo(20);
        assertThat(tank.amount()).isEqualTo(980);
    }

    @Test
    @DisplayName("fluid of one type cannot be transferred into a pipe holding another type")
    void rejectsMismatchedFluid() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        pipe.extract(new FakeFluidProvider(WATER, 100), 250); // pipe now holds 100 mB water, with room to spare

        FakeFluidProvider lava = new FakeFluidProvider(LAVA, 250);
        long extracted = pipe.extract(lava, 150); // try to top up with a different fluid
        assertThat(extracted).isZero(); // rejected: can't mix fluids
        assertThat(pipe.amount()).isEqualTo(100); // unchanged
        assertThat(pipe.fluid()).isEqualTo(WATER); // still water
        assertThat(lava.amount()).isEqualTo(250); // lava provider untouched
    }

    @Test
    @DisplayName("a pipe pushes fluid into a connected pipe until levels equalize")
    void pushEqualizesLevels() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 100);
        FluidPipe<String> destination = FluidPipe.extractor();

        long moved = source.push(destination, 100); // try to push a lot

        assertThat(moved).isEqualTo(50); // only enough to split the difference
        assertThat(source.amount()).isEqualTo(50);
        assertThat(destination.amount()).isEqualTo(50);
        assertThat(destination.fluid()).isEqualTo(WATER);
    }

    @Test
    @DisplayName("a push may leave the destination at most 1 mB fuller than the source")
    void pushLeavesDestinationAtMostOneMoreFull() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 101); // odd gap — the spare 1 mB goes across so the gap can close
        FluidPipe<String> destination = FluidPipe.extractor();

        long moved = source.push(destination, 200);

        assertThat(moved).isEqualTo(51); // rounds up so levels converge (no staircase)
        assertThat(source.amount()).isEqualTo(50);
        assertThat(destination.amount()).isEqualTo(51); // at most 1 more than the source
    }

    @Test
    @DisplayName("push moves no more than the requested rate")
    void pushLimitedByRequestedRate() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 250); // equalize gap (125) is larger than the rate
        FluidPipe<String> destination = FluidPipe.extractor();

        long moved = source.push(destination, 20); // one tick's transfer allowance

        assertThat(moved).isEqualTo(20); // rate caps before equalization does
        assertThat(source.amount()).isEqualTo(230);
        assertThat(destination.amount()).isEqualTo(20);
    }

    @Test
    @DisplayName("push won't move into a target that is already as full or fuller")
    void pushWontMoveIntoFullerTarget() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 50);
        FluidPipe<String> destination = FluidPipe.extractor();
        destination.restore(WATER, 100); // already fuller than the source

        long moved = source.push(destination, 100);

        assertThat(moved).isZero(); // no uphill flow
        assertThat(source.amount()).isEqualTo(50);
        assertThat(destination.amount()).isEqualTo(100);
    }

    @Test
    @DisplayName("push won't mix into a target holding a different fluid")
    void pushRejectsMismatchedFluid() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 200);
        FluidPipe<String> destination = FluidPipe.extractor();
        destination.restore(LAVA, 50); // emptier, so equalization would move if allowed

        long moved = source.push(destination, 200);

        assertThat(moved).isZero(); // rejected: can't mix fluids
        assertThat(source.amount()).isEqualTo(200); // unchanged
        assertThat(destination.amount()).isEqualTo(50); // unchanged
        assertThat(destination.fluid()).isEqualTo(LAVA); // still lava
    }

    @Test
    @DisplayName("a pipe pours fluid down to fill the pipe below, past its own level")
    void poursDownGreedily() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 100);
        FluidPipe<String> below = FluidPipe.extractor();

        long moved = source.pour(below, 100);

        assertThat(moved).isEqualTo(100); // all of it falls, not just half — gravity, not equalization
        assertThat(source.amount()).isZero();
        assertThat(below.amount()).isEqualTo(100); // below ends fuller than the source
    }

    @Test
    @DisplayName("pour can't overfill the pipe below")
    void pourLimitedByTargetCapacity() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 250);
        FluidPipe<String> below = FluidPipe.extractor();
        below.restore(WATER, 200); // only 50 mB of space left

        long moved = source.pour(below, 250);

        assertThat(moved).isEqualTo(50);
        assertThat(below.amount()).isEqualTo(250); // full, not overfilled
        assertThat(source.amount()).isEqualTo(200);
    }

    @Test
    @DisplayName("pour won't mix into a pipe holding a different fluid")
    void pourRejectsMismatchedFluid() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 200);
        FluidPipe<String> below = FluidPipe.extractor();
        below.restore(LAVA, 50);

        long moved = source.pour(below, 200);

        assertThat(moved).isZero();
        assertThat(source.amount()).isEqualTo(200);
        assertThat(below.amount()).isEqualTo(50);
        assertThat(below.fluid()).isEqualTo(LAVA);
    }

    @Test
    @DisplayName("a source fully drained by pouring resets to empty")
    void pourDrainedSourceResetsToEmpty() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 100);
        FluidPipe<String> below = FluidPipe.extractor();

        source.pour(below, 100);

        assertThat(source.amount()).isZero();
        assertThat(source.fluid()).isNull();
    }

    @Test
    @DisplayName("a full pipe lifts fluid up greedily when it has head")
    void liftsUpWhenFull() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 250); // full
        FluidPipe<String> above = FluidPipe.extractor();

        long moved = source.lift(above, 100, 8); // head available

        assertThat(moved).isEqualTo(100); // greedy, up to the requested amount
        assertThat(source.amount()).isEqualTo(150);
        assertThat(above.amount()).isEqualTo(100);
    }

    @Test
    @DisplayName("a pipe that isn't full won't lift fluid up")
    void doesNotLiftWhenNotFull() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 100); // partial — fluid pools and fills first, it doesn't climb yet
        FluidPipe<String> above = FluidPipe.extractor();

        long moved = source.lift(above, 100, 8); // has head, but isn't full

        assertThat(moved).isZero();
        assertThat(source.amount()).isEqualTo(100);
        assertThat(above.amount()).isZero();
    }

    @Test
    @DisplayName("a pipe with no head can't lift fluid up even when full")
    void noHeadNoLift() {
        FluidPipe<String> source = FluidPipe.extractor();
        source.restore(WATER, 250); // full, but no head
        FluidPipe<String> above = FluidPipe.extractor();

        long moved = source.lift(above, 100, 0); // no head — fluid won't climb

        assertThat(moved).isZero();
        assertThat(source.amount()).isEqualTo(250);
        assertThat(above.amount()).isZero();
    }

    @Test
    @DisplayName("extraction of a non-positive amount is a no-op and never touches the provider")
    void rejectsNonPositiveExtraction() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        FakeFluidProvider tank = new FakeFluidProvider(WATER, 500);

        assertThat(pipe.extract(tank, 0)).isZero();
        assertThat(pipe.extract(tank, -50)).isZero();

        assertThat(pipe.amount()).isZero();
        assertThat(pipe.fluid()).isNull();
        assertThat(tank.amount()).isEqualTo(500); // provider untouched
    }

    @Test
    @DisplayName("restore clamps an over-capacity amount down to the pipe's capacity")
    void restoreClampsToCapacity() {
        FluidPipe<String> pipe = FluidPipe.extractor();
        pipe.restore(WATER, 1000); // more than the 250 mB capacity
        assertThat(pipe.amount()).isEqualTo(250);
        assertThat(pipe.fluid()).isEqualTo(WATER);
    }

    /** A water provider with effectively unlimited fluid, for tests where the source isn't the constraint. */
    private static FakeFluidProvider ampleProvider() {
        return new FakeFluidProvider(WATER, 1_000_000);
    }
}
