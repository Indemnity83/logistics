package com.logistics.core.engine.block.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class RedstoneTargetGateTest {
    @Test
    void acceptsLowTierEnergy_rejectsNullTarget() {
        assertThat(RedstoneTargetGate.acceptsLowTierEnergy(null, Direction.NORTH)).isFalse();
    }

    @Test
    void acceptsLowTierEnergy_rejectsTargetWithoutLowTierContract() {
        assertThat(RedstoneTargetGate.acceptsLowTierEnergy(new Object(), Direction.NORTH)).isFalse();
    }

    @Test
    void acceptsLowTierEnergy_usesDefaultAcceptorContract() {
        assertThat(RedstoneTargetGate.acceptsLowTierEnergy(new DefaultAcceptor(), Direction.NORTH)).isTrue();
    }

    @Test
    void acceptsLowTierEnergy_passesOppositeOfEngineOutputDirectionToTarget() {
        RecordingAcceptor acceptor = new RecordingAcceptor(Direction.SOUTH);

        assertThat(RedstoneTargetGate.acceptsLowTierEnergy(acceptor, Direction.NORTH)).isTrue();
        assertThat(acceptor.lastDirection()).isEqualTo(Direction.SOUTH);
    }

    @Test
    void acceptsLowTierEnergy_rejectsWhenTargetRejectsInputSide() {
        RecordingAcceptor acceptor = new RecordingAcceptor(Direction.UP);

        assertThat(RedstoneTargetGate.acceptsLowTierEnergy(acceptor, Direction.NORTH)).isFalse();
        assertThat(acceptor.lastDirection()).isEqualTo(Direction.SOUTH);
    }

    private static final class DefaultAcceptor implements AcceptsLowTierEnergy {}

    private static final class RecordingAcceptor implements AcceptsLowTierEnergy {
        private final Direction acceptedDirection;
        private Direction lastDirection;

        private RecordingAcceptor(Direction acceptedDirection) {
            this.acceptedDirection = acceptedDirection;
        }

        @Override
        public boolean acceptsLowTierEnergyFrom(Direction from) {
            lastDirection = from;
            return from == acceptedDirection;
        }

        private Direction lastDirection() {
            return lastDirection;
        }
    }
}
