package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class EngineEnergyPusherTest {

    @Test
    void sendableAmount_isCappedByOutputRate() {
        assertThat(EngineEnergyPusher.sendableAmount(10, 1000)).isEqualTo(10);
    }

    @Test
    void sendableAmount_isCappedByBufferedEnergy() {
        assertThat(EngineEnergyPusher.sendableAmount(10, 3)).isEqualTo(3);
    }

    @Test
    void sendableAmount_isZeroWhenBufferEmpty() {
        assertThat(EngineEnergyPusher.sendableAmount(10, 0)).isEqualTo(0);
    }

    @Test
    void sendableAmount_neverNegative() {
        assertThat(EngineEnergyPusher.sendableAmount(-5, 100)).isEqualTo(0);
    }

    @Test
    void push_withNullLevel_sendsNothing() {
        // No live level (unit context): the pusher short-circuits instead of dereferencing it.
        long sent = EngineEnergyPusher.push(
                null, BlockPos.ZERO, Direction.NORTH, new com.logistics.core.lib.energy.EnergyComponent(1000, 0, Long.MAX_VALUE, () -> {}), 10);
        assertThat(sent).isEqualTo(0);
    }
}
