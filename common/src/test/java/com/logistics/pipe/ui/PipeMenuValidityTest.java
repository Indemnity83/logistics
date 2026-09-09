package com.logistics.pipe.ui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The reach every pipe menu and every pipe packet shares. Handlers used to disagree — some allowed
 * 64 blocks, some 8, some any distance at all — so the value is pinned here rather than repeated.
 */
@DisplayName("Pipe menu reach")
class PipeMenuValidityTest {

    private static double blocks(double distance) {
        return distance * distance;
    }

    @Test
    @DisplayName("matches vanilla's block interaction range")
    void matchesVanillaInteractionRange() {
        // isWithinBlockInteractionRange(pos, 4.0): the 4.5 block_interaction_range attribute
        // padded by 4, measured to the block AABB.
        assertThat(PipeMenuValidity.MAX_REACH).isEqualTo(8.0);
    }

    @Test
    @DisplayName("a player at the edge of reach keeps the menu open")
    void acceptsPlayerAtTheEdgeOfReach() {
        assertThat(PipeMenuValidity.isWithinReach(blocks(7.9))).isTrue();
        assertThat(PipeMenuValidity.isWithinReach(blocks(8.0))).isTrue();
    }

    @Test
    @DisplayName("a player past reach does not")
    void rejectsPlayerPastReach() {
        assertThat(PipeMenuValidity.isWithinReach(blocks(8.1))).isFalse();
    }

    @Test
    @DisplayName("64 blocks away is out of reach, not in it")
    void rejectsTheOldSixtyFourBlockReach() {
        // Three handlers compared against 64.0 * 64.0 — a squared comparison, so 64 blocks rather
        // than the 8 the expression was copied from.
        assertThat(PipeMenuValidity.isWithinReach(blocks(64.0))).isFalse();
    }
}
