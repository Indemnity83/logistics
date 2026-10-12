package com.logistics.core.machine.area;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WorkAreaTest {

    private static final BlockPos ORIGIN = new BlockPos(100, 64, -200);

    /**
     * Facing, size, expected first plot (near row, left end), expected step along a row (left to
     * right), expected step between rows (outward).
     */
    static Stream<Arguments> layouts() {
        Stream.Builder<Arguments> out = Stream.builder();
        for (int size : new int[] {9, 13, 17}) {
            int h = size / 2;
            out.add(Arguments.of(Direction.NORTH, size, ORIGIN.offset(-h, 0, -1), new int[] {1, 0}, new int[] {0, -1}));
            out.add(Arguments.of(Direction.SOUTH, size, ORIGIN.offset(h, 0, 1), new int[] {-1, 0}, new int[] {0, 1}));
            out.add(Arguments.of(Direction.EAST, size, ORIGIN.offset(1, 0, -h), new int[] {0, 1}, new int[] {1, 0}));
            out.add(Arguments.of(Direction.WEST, size, ORIGIN.offset(-1, 0, h), new int[] {0, -1}, new int[] {-1, 0}));
        }
        return out.build();
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void plotsFollowRowMajorOrderFromNearLeft(
            Direction facing, int size, BlockPos first, int[] alongRow, int[] outward) {
        WorkArea area = WorkArea.inFront(ORIGIN, facing, size);

        assertThat(area.size()).isEqualTo(size);
        assertThat(area.plotCount()).isEqualTo(size * size);
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                BlockPos expected = first.offset(
                        col * alongRow[0] + row * outward[0], 0, col * alongRow[1] + row * outward[1]);
                assertThat(area.plot(row * size + col)).isEqualTo(expected);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void nearRowIsAdjacentAndCentredOnOrigin(
            Direction facing, int size, BlockPos first, int[] alongRow, int[] outward) {
        WorkArea area = WorkArea.inFront(ORIGIN, facing, size);

        assertThat(area.plot(size / 2)).isEqualTo(ORIGIN.relative(facing));
        assertThat(area.contains(ORIGIN)).isFalse();
        assertThat(area.contains(ORIGIN.relative(facing.getOpposite()))).isFalse();
        assertThat(area.contains(ORIGIN.relative(facing, size + 1))).isFalse();
        assertThat(area.contains(ORIGIN.relative(facing, size))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void cornersBoundEveryPlot(Direction facing, int size, BlockPos first, int[] alongRow, int[] outward) {
        WorkArea area = WorkArea.inFront(ORIGIN, facing, size);
        BlockPos min = area.min();
        BlockPos max = area.max();

        assertThat(max.getX() - min.getX() + 1).isEqualTo(size);
        assertThat(max.getZ() - min.getZ() + 1).isEqualTo(size);
        assertThat(min.getY()).isEqualTo(ORIGIN.getY());
        assertThat(max.getY()).isEqualTo(ORIGIN.getY());
        for (int i = 0; i < area.plotCount(); i++) {
            BlockPos p = area.plot(i);
            assertThat(p.getX()).isBetween(min.getX(), max.getX());
            assertThat(p.getZ()).isBetween(min.getZ(), max.getZ());
        }
        assertThat(area.contains(min)).isTrue();
        assertThat(area.contains(max)).isTrue();
        assertThat(area.contains(min.offset(-1, 0, 0))).isFalse();
        assertThat(area.contains(min.offset(0, 0, -1))).isFalse();
        assertThat(area.contains(max.offset(1, 0, 0))).isFalse();
        assertThat(area.contains(max.offset(0, 0, 1))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void indexAndPositionRoundTrip(Direction facing, int size, BlockPos first, int[] alongRow, int[] outward) {
        WorkArea area = WorkArea.inFront(ORIGIN, facing, size);
        Set<BlockPos> seen = new HashSet<>();

        for (int i = 0; i < area.plotCount(); i++) {
            BlockPos p = area.plot(i);
            assertThat(area.indexOf(p)).isEqualTo(i);
            assertThat(seen.add(p)).isTrue();
        }
        BlockPos min = area.min();
        BlockPos max = area.max();
        for (int x = min.getX() - 1; x <= max.getX() + 1; x++) {
            for (int z = min.getZ() - 1; z <= max.getZ() + 1; z++) {
                BlockPos p = new BlockPos(x, ORIGIN.getY(), z);
                boolean inside = x >= min.getX() && x <= max.getX() && z >= min.getZ() && z <= max.getZ();
                assertThat(area.indexOf(p) >= 0).isEqualTo(inside);
            }
        }
    }

    @Test
    void containsAndIndexOfIgnoreY() {
        WorkArea area = WorkArea.inFront(ORIGIN, Direction.NORTH, 9);
        BlockPos plot = area.plot(40);

        assertThat(area.contains(plot.above(5))).isTrue();
        assertThat(area.contains(plot.below(70))).isTrue();
        assertThat(area.indexOf(plot.above(3))).isEqualTo(40);
        assertThat(area.contains(area.max().offset(1, 10, 0))).isFalse();
    }

    @Test
    void rangeBonusGrowsSizeAroundSameOrigin() {
        WorkArea base = WorkArea.inFront(ORIGIN, Direction.EAST, 9);
        WorkArea once = base.withRangeBonus(2);
        WorkArea twice = once.withRangeBonus(2);

        assertThat(once.size()).isEqualTo(13);
        assertThat(twice.size()).isEqualTo(17);
        assertThat(base.withRangeBonus(0)).isEqualTo(base);
        assertThat(twice.origin()).isEqualTo(ORIGIN);
        assertThat(twice.facing()).isEqualTo(Direction.EAST);
        assertThat(twice.plot(17 / 2)).isEqualTo(ORIGIN.relative(Direction.EAST));
        assertThatThrownBy(() -> base.withRangeBonus(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sizeOneIsTheSingleBlockInFront() {
        WorkArea area = WorkArea.inFront(ORIGIN, Direction.SOUTH, 1);

        assertThat(area.plotCount()).isEqualTo(1);
        assertThat(area.plot(0)).isEqualTo(ORIGIN.south());
        assertThat(area.min()).isEqualTo(ORIGIN.south());
        assertThat(area.max()).isEqualTo(ORIGIN.south());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -3, 2, 8, 10})
    void rejectsEvenOrNonPositiveSizes(int size) {
        assertThatThrownBy(() -> WorkArea.inFront(ORIGIN, Direction.NORTH, size))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsVerticalFacing() {
        assertThatThrownBy(() -> WorkArea.inFront(ORIGIN, Direction.UP, 9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WorkArea.inFront(ORIGIN, Direction.DOWN, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plotRejectsOutOfRangeIndex() {
        WorkArea area = WorkArea.inFront(ORIGIN, Direction.NORTH, 9);

        assertThatThrownBy(() -> area.plot(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> area.plot(81)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
