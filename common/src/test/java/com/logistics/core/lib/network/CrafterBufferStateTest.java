package com.logistics.core.lib.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CrafterBufferState")
class CrafterBufferStateTest {

    @Test
    @DisplayName("safeBatchCapacity is limited by input insert space")
    void limitedByInserts() {
        assertThat(new CrafterBufferState(3, 10).safeBatchCapacity()).isEqualTo(3);
    }

    @Test
    @DisplayName("safeBatchCapacity is limited by output space")
    void limitedByOutputSpace() {
        assertThat(new CrafterBufferState(10, 4).safeBatchCapacity()).isEqualTo(4);
    }

    @Test
    @DisplayName("safeBatchCapacity equals either limit when they match")
    void equalLimits() {
        assertThat(new CrafterBufferState(5, 5).safeBatchCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("FULL has no capacity")
    void fullHasNoCapacity() {
        assertThat(CrafterBufferState.FULL.safeBatchCapacity()).isZero();
    }

    @Test
    @DisplayName("UNLIMITED reports the maximum capacity")
    void unlimitedIsMax() {
        assertThat(CrafterBufferState.UNLIMITED.safeBatchCapacity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("rejects a negative insert limit")
    void rejectsNegativeInserts() {
        assertThatThrownBy(() -> new CrafterBufferState(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects a negative output-space limit")
    void rejectsNegativeOutputSpace() {
        assertThatThrownBy(() -> new CrafterBufferState(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
