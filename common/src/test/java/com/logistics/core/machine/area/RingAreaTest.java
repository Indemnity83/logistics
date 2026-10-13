package com.logistics.core.machine.area;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RingAreaTest {

    private static final BlockPos ORIGIN = new BlockPos(100, 64, -200);

    static Stream<Arguments> shapes() {
        return Stream.of(
                Arguments.of(1, 3),
                Arguments.of(1, 5),
                Arguments.of(3, 5),
                Arguments.of(9, 13),
                Arguments.of(9, 15),
                Arguments.of(5, 17));
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void plotCountIsOuterSquareLessInnerSquare(int inner, int outer) {
        assertThat(new RingArea(ORIGIN, inner, outer).plotCount()).isEqualTo(outer * outer - inner * inner);
    }

    @Test
    void knownPlotCounts() {
        assertThat(new RingArea(ORIGIN, 9, 15).plotCount()).isEqualTo(144);
        assertThat(new RingArea(ORIGIN, 9, 13).plotCount()).isEqualTo(88);
        assertThat(new RingArea(ORIGIN, 1, 5).plotCount()).isEqualTo(24);
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void laneLengthsArePerimetersAndSumToPlotCount(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);

        assertThat(area.laneCount()).isEqualTo((outer - inner) / 2);
        int total = 0;
        for (int lane = 0; lane < area.laneCount(); lane++) {
            int side = inner + 2 + 2 * lane;
            assertThat(area.laneLength(lane)).isEqualTo(4 * (side - 1));
            total += area.laneLength(lane);
        }
        assertThat(total).isEqualTo(area.plotCount());
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void indexingIsLaneMajor(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);

        int i = 0;
        for (int lane = 0; lane < area.laneCount(); lane++) {
            for (int step = 0; step < area.laneLength(lane); step++, i++) {
                assertThat(area.index(lane, step)).isEqualTo(i);
                assertThat(area.lane(i)).isEqualTo(lane);
                assertThat(area.step(i)).isEqualTo(step);
                assertThat(area.plot(i)).isEqualTo(area.plot(lane, step));
            }
        }
        assertThat(i).isEqualTo(area.plotCount());
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void indexAndPositionRoundTrip(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);
        Set<BlockPos> seen = new HashSet<>();

        for (int i = 0; i < area.plotCount(); i++) {
            BlockPos p = area.plot(i);
            assertThat(p.getY()).isEqualTo(ORIGIN.getY());
            assertThat(area.indexOf(p)).isEqualTo(i);
            assertThat(seen.add(p)).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void membershipIsTheRingOnly(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);
        int hi = inner / 2;
        int ho = outer / 2;

        for (int dx = -ho - 2; dx <= ho + 2; dx++) {
            for (int dz = -ho - 2; dz <= ho + 2; dz++) {
                int d = Math.max(Math.abs(dx), Math.abs(dz));
                boolean inRing = d > hi && d <= ho;
                BlockPos p = ORIGIN.offset(dx, 0, dz);
                assertThat(area.contains(p)).as("(%d, %d)", dx, dz).isEqualTo(inRing);
                if (!inRing) {
                    assertThat(area.indexOf(p)).isEqualTo(-1);
                }
            }
        }
        assertThat(area.indexOf(ORIGIN)).isEqualTo(-1);
    }

    @Test
    void containsAndIndexOfIgnoreY() {
        RingArea area = new RingArea(ORIGIN, 9, 15);
        BlockPos plot = area.plot(100);

        assertThat(area.contains(plot.above(5))).isTrue();
        assertThat(area.contains(plot.below(70))).isTrue();
        assertThat(area.indexOf(plot.above(3))).isEqualTo(100);
        assertThat(area.contains(ORIGIN.above(2))).isFalse();
        assertThat(area.contains(area.max().offset(1, 10, 0))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void stepsWalkClockwiseFromNorthWestCorner(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);

        for (int lane = 0; lane < area.laneCount(); lane++) {
            int h = inner / 2 + 1 + lane;
            int length = area.laneLength(lane);
            assertThat(area.plot(lane, 0)).isEqualTo(ORIGIN.offset(-h, 0, -h));
            assertThat(area.plot(lane, 1)).isEqualTo(ORIGIN.offset(-h + 1, 0, -h));
            assertThat(area.plot(lane, 2 * h)).isEqualTo(ORIGIN.offset(h, 0, -h));
            assertThat(area.plot(lane, 4 * h)).isEqualTo(ORIGIN.offset(h, 0, h));
            assertThat(area.plot(lane, 6 * h)).isEqualTo(ORIGIN.offset(-h, 0, h));
            assertThat(area.plot(lane, length - 1)).isEqualTo(ORIGIN.offset(-h, 0, -h + 1));
            for (int step = 0; step < length; step++) {
                BlockPos here = area.plot(lane, step);
                BlockPos next = area.plot(lane, (step + 1) % length);
                assertThat(here.distManhattan(next)).as("lane %d step %d", lane, step).isEqualTo(1);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void cornersAreTheOuterSquare(int inner, int outer) {
        RingArea area = new RingArea(ORIGIN, inner, outer);
        int ho = outer / 2;

        assertThat(area.min()).isEqualTo(ORIGIN.offset(-ho, 0, -ho));
        assertThat(area.max()).isEqualTo(ORIGIN.offset(ho, 0, ho));
        assertThat(area.contains(area.min())).isTrue();
        assertThat(area.contains(area.max())).isTrue();
    }

    @Test
    void storesAnImmutableOrigin() {
        BlockPos.MutableBlockPos mutable = ORIGIN.mutable();
        RingArea area = new RingArea(mutable, 1, 3);
        mutable.move(5, 0, 5);

        assertThat(area.origin()).isEqualTo(ORIGIN);
    }

    @Test
    void rejectsInvalidShapes() {
        assertThatThrownBy(() -> new RingArea(null, 1, 3)).isInstanceOf(IllegalArgumentException.class);
        int[][] invalid = {{0, 3}, {-1, 3}, {2, 5}, {3, 6}, {3, 3}, {5, 3}, {1, 1}, {1, 2}};
        for (int[] shape : invalid) {
            assertThatThrownBy(() -> new RingArea(ORIGIN, shape[0], shape[1]))
                    .as("inner %d outer %d", shape[0], shape[1])
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsOutOfRangeIndices() {
        RingArea area = new RingArea(ORIGIN, 9, 13);

        assertThatThrownBy(() -> area.plot(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.plot(88)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.lane(88)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.plot(2, 0)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.plot(0, 40)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.index(1, -1)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
