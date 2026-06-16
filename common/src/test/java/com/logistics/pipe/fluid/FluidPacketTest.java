package com.logistics.pipe.fluid;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fluid packet transport helpers")
class FluidPacketTest {

    @Test
    @DisplayName("split spreads evenly, conserves the total, and respects per-output room")
    void split() {
        assertThat(FluidSplit.split(20, new long[] {250, 250})).containsExactly(10, 10);
        long[] three = FluidSplit.split(20, new long[] {250, 250, 250});
        assertThat(three[0] + three[1] + three[2]).isEqualTo(20);
        assertThat(three).containsExactly(7, 7, 6); // remainder to the earliest outputs
        long[] capped = FluidSplit.split(20, new long[] {3, 250});
        assertThat(capped[0] + capped[1]).isEqualTo(20);
        assertThat(capped[0]).isEqualTo(3); // tight output capped; the rest absorbs the overflow
        assertThat(FluidSplit.split(20, new long[] {0, 0})).containsExactly(0, 0); // nowhere to go
    }

    @Test
    @DisplayName("a parcel dwells its countdown, then is ready to hop and never goes negative")
    void parcelDwell() {
        // The dwell logic doesn't touch the fluid identity, so a null key keeps this off the MC registry.
        TravelingFluid parcel = new TravelingFluid(null, 50, Direction.NORTH, 3);
        assertThat(parcel.ready()).isFalse();
        assertThat(parcel.tickCountdown()).isFalse(); // 3 -> 2
        assertThat(parcel.tickCountdown()).isFalse(); // 2 -> 1
        assertThat(parcel.tickCountdown()).isTrue(); // 1 -> 0, ready
        assertThat(parcel.tickCountdown()).isTrue(); // stays ready, never negative
        assertThat(parcel.countdown()).isZero();
    }
}
