package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.FrameLayout;
import com.logistics.automation.laserquarry.entity.QuarryBounds;
import com.logistics.automation.laserquarry.entity.QuarryFrameRect;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FrameLayout")
class FrameLayoutTest extends MinecraftTestEnvironment {

    private static final BlockPos QUARRY = new BlockPos(0, 64, 0);
    private static final int AREA = 16;

    @Nested
    @DisplayName("ringSize")
    class RingSize {

        @Test
        @DisplayName("computes perimeter of a square (4×4 → 12)")
        void square4x4() {
            assertThat(FrameLayout.ringSize(4, 4)).isEqualTo(12);
        }

        @Test
        @DisplayName("computes perimeter of the default 16×16 ring → 60")
        void default16x16() {
            assertThat(FrameLayout.ringSize(16, 16)).isEqualTo(60);
        }

        @Test
        @DisplayName("handles non-square rectangles (4×8 → 20)")
        void rect4x8() {
            assertThat(FrameLayout.ringSize(4, 8)).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("ringPosition")
    class RingPositionTraversal {

        @Test
        @DisplayName("starts at the NW corner, walks the north edge first (W→E)")
        void firstStepIsNorthWestCorner() {
            BlockPos p = FrameLayout.ringPosition(0, 0, 0, 3, 3, 5);

            assertThat(p).isEqualTo(new BlockPos(0, 5, 0));
        }

        @Test
        @DisplayName("walks east along the north edge")
        void walksNorthEdge() {
            BlockPos p = FrameLayout.ringPosition(2, 0, 0, 3, 3, 5);

            assertThat(p).isEqualTo(new BlockPos(2, 5, 0));
        }

        @Test
        @DisplayName("turns south at the NE corner")
        void turnsSouthAtNECorner() {
            // width=4, so index 4 is one past the north edge → east edge index 0 → z = startZ+1.
            BlockPos p = FrameLayout.ringPosition(4, 0, 0, 3, 3, 5);

            assertThat(p).isEqualTo(new BlockPos(3, 5, 1));
        }

        @Test
        @DisplayName("walks west along the south edge from the SE corner")
        void walksSouthEdgeWest() {
            // After 4 (north) + 3 (east) = 7 entries, index 7 is south edge index 0 → endX-1.
            BlockPos p = FrameLayout.ringPosition(7, 0, 0, 3, 3, 5);

            assertThat(p).isEqualTo(new BlockPos(2, 5, 3));
        }

        @Test
        @DisplayName("walks north along the west edge from the SW corner")
        void walksWestEdgeNorth() {
            // 4 + 3 + 3 = 10. Index 10 is west edge index 0 → z = endZ-1 = 2.
            BlockPos p = FrameLayout.ringPosition(10, 0, 0, 3, 3, 5);

            assertThat(p).isEqualTo(new BlockPos(0, 5, 2));
        }

        @Test
        @DisplayName("returns null when index is past the perimeter")
        void nullPastPerimeter() {
            assertThat(FrameLayout.ringPosition(12, 0, 0, 3, 3, 5)).isNull();
        }
    }

    @Nested
    @DisplayName("nextFramePosition")
    class NextFramePosition {

        @Test
        @DisplayName("first frame block is the NW corner of the bottom ring")
        void firstIsBottomNWCorner() {
            BlockPos p = FrameLayout.nextFramePosition(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, 0);

            // NORTH: startX = -8, startZ = -16, bottomY = 64.
            assertThat(p).isEqualTo(new BlockPos(-8, 64, -16));
        }

        @Test
        @DisplayName("after the bottom ring, the first pillar starts at quarryY+1 at the NW corner")
        void firstPillarStartsAboveNWCorner() {
            int ringSize = FrameLayout.ringSize(AREA, AREA);
            BlockPos p =
                    FrameLayout.nextFramePosition(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, ringSize);

            assertThat(p).isEqualTo(new BlockPos(-8, 65, -16));
        }

        @Test
        @DisplayName("after pillars, the first top-ring block is the NW corner at quarryY+4")
        void topRingStartsAtNWCornerY4() {
            int ringSize = FrameLayout.ringSize(AREA, AREA);
            int topRingStartIndex = ringSize + FrameLayout.PILLAR_COUNT;

            BlockPos p = FrameLayout.nextFramePosition(
                    Direction.NORTH, QUARRY, new QuarryBounds(), AREA, topRingStartIndex);

            assertThat(p).isEqualTo(new BlockPos(-8, 68, -16));
        }

        @Test
        @DisplayName("returns null past the full frame build sequence")
        void nullPastEnd() {
            int totalBlocks = FrameLayout.ringSize(AREA, AREA) * 2 + FrameLayout.PILLAR_COUNT;

            assertThat(FrameLayout.nextFramePosition(
                            Direction.NORTH, QUARRY, new QuarryBounds(), AREA, totalBlocks))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("framePositions")
    class FramePositions {

        /** The same sequence BUILDING_FRAME walks, collected the long way. */
        private List<BlockPos> builtSequence(Direction facing, QuarryBounds bounds, int area) {
            List<BlockPos> built = new ArrayList<>();
            for (int index = 0; ; index++) {
                BlockPos pos = FrameLayout.nextFramePosition(facing, QUARRY, bounds, area, index);
                if (pos == null) {
                    return built;
                }
                built.add(pos);
            }
        }

        @Test
        @DisplayName("is exactly what the build phase places, in build order")
        void matchesTheBuildSequence() {
            List<BlockPos> positions = FrameLayout.framePositions(Direction.NORTH, QUARRY, new QuarryBounds(), AREA);

            // Teardown walks this list; anything the build placed but this misses is stranded.
            assertThat(positions)
                    .containsExactlyElementsOf(builtSequence(Direction.NORTH, new QuarryBounds(), AREA))
                    .hasSize(FrameLayout.ringSize(AREA, AREA) * 2 + FrameLayout.PILLAR_COUNT)
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("covers custom bounds too")
        void coversCustomBounds() {
            QuarryBounds bounds = new QuarryBounds();
            bounds.setCustom(-40, -70, -11, -41);

            List<BlockPos> positions = FrameLayout.framePositions(Direction.NORTH, QUARRY, bounds, AREA);

            assertThat(positions)
                    .containsExactlyElementsOf(builtSequence(Direction.NORTH, bounds, AREA))
                    .hasSize(FrameLayout.ringSize(30, 30) * 2 + FrameLayout.PILLAR_COUNT);
        }

        @Test
        @DisplayName("is empty when the rectangle cannot be resolved")
        void emptyWhenUnresolvable() {
            assertThat(FrameLayout.framePositions(Direction.UP, QUARRY, new QuarryBounds(), AREA))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("computeArms")
    class ComputeArms {

        // A 4×4 rectangle at the bottom layer (y=0), so corners are (0,0,0), (3,0,0), (3,0,3), (0,0,3).
        // bottomY = 0, topY = 4.
        private final QuarryFrameRect rect = new QuarryFrameRect(0, 0, 3, 3, 0, 4);

        @Test
        @DisplayName("NW corner at the bottom ring exposes up (no down) and inward edges")
        void bottomNWCorner() {
            boolean[] arms = FrameLayout.computeArms(rect, new BlockPos(0, 0, 0));

            // [north, south, east, west, up, down]
            assertThat(arms[0]).as("north").isFalse();
            assertThat(arms[1]).as("south").isTrue();
            assertThat(arms[2]).as("east").isTrue();
            assertThat(arms[3]).as("west").isFalse();
            assertThat(arms[4]).as("up").isTrue();
            assertThat(arms[5]).as("down").isFalse();
        }

        @Test
        @DisplayName("NW corner at the top ring exposes down (no up) and inward edges")
        void topNWCorner() {
            boolean[] arms = FrameLayout.computeArms(rect, new BlockPos(0, 4, 0));

            assertThat(arms[0]).as("north").isFalse();
            assertThat(arms[1]).as("south").isTrue();
            assertThat(arms[2]).as("east").isTrue();
            assertThat(arms[3]).as("west").isFalse();
            assertThat(arms[4]).as("up").isFalse();
            assertThat(arms[5]).as("down").isTrue();
        }

        @Test
        @DisplayName("mid-edge bottom block exposes only the two horizontal neighbours")
        void midEdgeBottom() {
            // (1,0,0) is on the north edge between corners.
            boolean[] arms = FrameLayout.computeArms(rect, new BlockPos(1, 0, 0));

            assertThat(arms[0]).as("north").isFalse();
            assertThat(arms[1]).as("south").isFalse();
            assertThat(arms[2]).as("east").isTrue();
            assertThat(arms[3]).as("west").isTrue();
            assertThat(arms[4]).as("up").isFalse();
            assertThat(arms[5]).as("down").isFalse();
        }

        @Test
        @DisplayName("mid-pillar between corners exposes up and down only")
        void midPillar() {
            // Corner column at y=2 (between bottomY=0 and topY=4).
            boolean[] arms = FrameLayout.computeArms(rect, new BlockPos(0, 2, 0));

            assertThat(arms[0]).as("north").isFalse();
            assertThat(arms[1]).as("south").isFalse();
            assertThat(arms[2]).as("east").isFalse();
            assertThat(arms[3]).as("west").isFalse();
            assertThat(arms[4]).as("up").isTrue();
            assertThat(arms[5]).as("down").isTrue();
        }
    }
}
