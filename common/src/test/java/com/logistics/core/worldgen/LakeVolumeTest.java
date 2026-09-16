package com.logistics.core.worldgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link LakeVolume} has to match vanilla's lake grid exactly, because {@link OilSeepFeature} uses it both
 * to find the ore the lake just laid and to decide which ore was already there. Scanning wide re-rolls
 * blocks the lake never wrote (a neighbouring deposit's ore); scanning narrow leaves part of the bank a
 * solid casing. Neither shows up as a crash — only as wrong-looking terrain in a rarely generated feature.
 *
 * <p>The grid is the one vanilla {@code LakeFeature} fills: a {@code boolean[2048]} addressed as
 * 16x16x8, written through {@code origin.offset(-8, -4, -8).offset(x, y, z)}.
 */
@DisplayName("Lake volume")
class LakeVolumeTest {

    private static final int VANILLA_GRID_X = 16;
    private static final int VANILLA_GRID_Y = 8;
    private static final int VANILLA_GRID_Z = 16;
    private static final int VANILLA_GRID_CELLS = VANILLA_GRID_X * VANILLA_GRID_Y * VANILLA_GRID_Z;

    private record Offset(int dx, int dy, int dz, int index) {}

    private static List<Offset> visited() {
        List<Offset> offsets = new ArrayList<>();
        LakeVolume.forEach((dx, dy, dz, index) -> offsets.add(new Offset(dx, dy, dz, index)));
        return offsets;
    }

    @Test
    @DisplayName("spans exactly the 16x16x8 grid the lake writes")
    void spansTheLakeGrid() {
        List<Offset> offsets = visited();

        assertThat(offsets).hasSize(VANILLA_GRID_CELLS);
        assertThat(offsets).extracting(Offset::dx).containsOnly(range(-8, VANILLA_GRID_X));
        assertThat(offsets).extracting(Offset::dy).containsOnly(range(-4, VANILLA_GRID_Y));
        assertThat(offsets).extracting(Offset::dz).containsOnly(range(-8, VANILLA_GRID_Z));
    }

    @Test
    @DisplayName("visits every position once")
    void visitsEveryPositionOnce() {
        assertThat(visited())
                .extracting(offset -> List.of(offset.dx(), offset.dy(), offset.dz()))
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("indexes a SIZE-length array with no gaps or collisions")
    void indexesTheArrayExactly() {
        List<Offset> offsets = visited();

        assertThat(offsets).extracting(Offset::index).doesNotHaveDuplicates();
        assertThat(offsets).extracting(Offset::index).allMatch(i -> i >= 0 && i < LakeVolume.SIZE);
        assertThat(offsets).hasSize(LakeVolume.SIZE);
    }

    @Test
    @DisplayName("hands the visitor the same index as a direct lookup")
    void visitorIndexMatchesDirectLookup() {
        assertThat(visited())
                .allSatisfy(offset ->
                        assertThat(LakeVolume.index(offset.dx(), offset.dy(), offset.dz())).isEqualTo(offset.index()));
    }

    private static Integer[] range(int min, int size) {
        Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++) {
            values[i] = min + i;
        }
        return values;
    }
}
