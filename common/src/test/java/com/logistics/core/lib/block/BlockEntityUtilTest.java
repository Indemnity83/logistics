package com.logistics.core.lib.block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BlockEntityUtil.splitIntoStacks")
class BlockEntityUtilTest {

    @Test
    @DisplayName("a non-positive amount yields no stacks")
    void nonPositiveAmountYieldsNothing() {
        assertThat(BlockEntityUtil.splitIntoStacks(0, 64)).isEmpty();
        assertThat(BlockEntityUtil.splitIntoStacks(-5, 64)).isEmpty();
    }

    @Test
    @DisplayName("an amount below the max is a single partial stack")
    void belowMaxIsOnePartialStack() {
        assertThat(BlockEntityUtil.splitIntoStacks(5, 64)).containsExactly(5);
    }

    @Test
    @DisplayName("an exact multiple splits into full stacks with no remainder")
    void exactMultipleHasNoRemainder() {
        assertThat(BlockEntityUtil.splitIntoStacks(128, 64)).containsExactly(64, 64);
    }

    @Test
    @DisplayName("a non-multiple ends with the remainder chunk")
    void nonMultipleEndsWithRemainder() {
        assertThat(BlockEntityUtil.splitIntoStacks(130, 64)).containsExactly(64, 64, 2);
    }

    @Test
    @DisplayName("a single-item max size produces one chunk per item")
    void singleItemMaxProducesOnePerItem() {
        assertThat(BlockEntityUtil.splitIntoStacks(3, 1)).containsExactly(1, 1, 1);
    }

    @Test
    @DisplayName("a non-positive max stack size is rejected (would never make progress)")
    void nonPositiveMaxIsRejected() {
        assertThatThrownBy(() -> BlockEntityUtil.splitIntoStacks(5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BlockEntityUtil.splitIntoStacks(5, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
