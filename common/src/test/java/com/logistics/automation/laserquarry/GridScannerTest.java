package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.GridScanner;
import com.logistics.automation.laserquarry.entity.QuarryBounds;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GridScanner")
class GridScannerTest extends MinecraftTestEnvironment {

    private static final BlockPos QUARRY = new BlockPos(0, 64, 0);
    private static final int AREA = 16;

    @Nested
    @DisplayName("clearingTarget")
    class ClearingTarget {

        @Test
        @DisplayName("returns top layer position above the quarry for NORTH facing at grid origin")
        void northTopLayerAtOrigin() {
            BlockPos target =
                    GridScanner.clearingTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, 0, 0, 0);

            // NORTH: startX = -8, startZ = -16. topY = 64 + 4 = 68.
            assertThat(target).isEqualTo(new BlockPos(-8, 68, -16));
        }

        @Test
        @DisplayName("descends one block per gridY step")
        void descendsWithGridY() {
            BlockPos target =
                    GridScanner.clearingTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, 0, 3, 0);

            assertThat(target.getY()).isEqualTo(65);
        }

        @Test
        @DisplayName("returns null when gridY walks below the quarry's own level")
        void nullWhenBelowQuarryLevel() {
            // 5 layers down from topY=68 → currentY=63 < bottomY=64
            BlockPos target =
                    GridScanner.clearingTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, 0, 5, 0);

            assertThat(target).isNull();
        }

        @Test
        @DisplayName("mirrors the area when facing SOUTH")
        void southMirrors() {
            BlockPos target =
                    GridScanner.clearingTarget(Direction.SOUTH, QUARRY, new QuarryBounds(), AREA, 0, 0, 0);

            // SOUTH: startX = -8, startZ = +1.
            assertThat(target).isEqualTo(new BlockPos(-8, 68, 1));
        }

        @Test
        @DisplayName("respects custom bounds, ignoring facing")
        void usesCustomBounds() {
            QuarryBounds bounds = new QuarryBounds();
            bounds.setCustom(100, 100, 110, 115);

            BlockPos target = GridScanner.clearingTarget(Direction.NORTH, QUARRY, bounds, AREA, 2, 0, 3);

            assertThat(target).isEqualTo(new BlockPos(102, 68, 103));
        }

        @Test
        @DisplayName("returns null for non-horizontal facings")
        void nullForVerticalFacing() {
            assertThat(GridScanner.clearingTarget(Direction.UP, QUARRY, new QuarryBounds(), AREA, 0, 0, 0))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("miningTarget")
    class MiningTarget {

        @Test
        @DisplayName("starts one block below the quarry inset 1 from the NORTH frame")
        void firstMiningPosition() {
            BlockPos target =
                    GridScanner.miningTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, -64, 0, 0, 0);

            // NORTH: startX_inner = -7, startZ_inner = -15, currentY = 63.
            assertThat(target).isEqualTo(new BlockPos(-7, 63, -15));
        }

        @Test
        @DisplayName("zigzags X back when totalRows is odd")
        void xZigzagOnOddRow() {
            // gridY=0, gridZ=1 → totalRows=1 (odd) → targetX = startX_inner + (innerWidth-1-gridX).
            BlockPos target =
                    GridScanner.miningTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, -64, 0, 0, 1);

            // innerWidth = 14, startX_inner=-7, so targetX = -7 + (14-1-0) = 6, targetZ = -15 + 1 = -14.
            assertThat(target).isEqualTo(new BlockPos(6, 63, -14));
        }

        @Test
        @DisplayName("zigzags Z back on odd layers")
        void zZigzagOnOddLayer() {
            // gridY=1 (odd layer) → targetZ = startZ_inner + (innerDepth-1-gridZ).
            BlockPos target =
                    GridScanner.miningTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, -64, 0, 1, 0);

            assertThat(target.getZ()).isEqualTo(-15 + (14 - 1 - 0));
            assertThat(target.getY()).isEqualTo(62); // 63 - 1
        }

        @Test
        @DisplayName("returns null when currentY drops below worldMinY")
        void nullBelowWorldMin() {
            BlockPos target =
                    GridScanner.miningTarget(Direction.NORTH, QUARRY, new QuarryBounds(), AREA, 100, 0, 0, 0);

            // currentY = 63 < 100 → null
            assertThat(target).isNull();
        }

        @Test
        @DisplayName("returns null when custom bounds leave no inner area")
        void nullWhenInnerAreaEmpty() {
            QuarryBounds bounds = new QuarryBounds();
            bounds.setCustom(0, 0, 1, 1); // 2×2 frame → no interior

            BlockPos target = GridScanner.miningTarget(Direction.NORTH, QUARRY, bounds, AREA, -64, 0, 0, 0);

            assertThat(target).isNull();
        }

        @Test
        @DisplayName("respects custom bounds for the mining inset")
        void usesCustomBoundsForInset() {
            QuarryBounds bounds = new QuarryBounds();
            bounds.setCustom(100, 100, 105, 105); // 6×6 frame → 4×4 interior

            BlockPos target = GridScanner.miningTarget(Direction.NORTH, QUARRY, bounds, AREA, -64, 0, 0, 0);

            // startX_inner = 101, startZ_inner = 101, currentY = 63.
            assertThat(target).isEqualTo(new BlockPos(101, 63, 101));
        }
    }

    @Nested
    @DisplayName("shouldSkip")
    class ShouldSkip {

        @Test
        @DisplayName("skips air blocks")
        void skipsAir() {
            BlockState air = Blocks.AIR.defaultBlockState();

            assertThat(GridScanner.shouldSkip(null, BlockPos.ZERO, air)).isTrue();
        }

        @Test
        @DisplayName("skips fluid blocks (water)")
        void skipsWater() {
            BlockState water = Blocks.WATER.defaultBlockState();

            assertThat(GridScanner.shouldSkip(null, BlockPos.ZERO, water)).isTrue();
        }

        @Test
        @DisplayName("skips fluid blocks (lava) — hazard handling is a separate concern for callers")
        void skipsLava() {
            BlockState lava = Blocks.LAVA.defaultBlockState();

            assertThat(GridScanner.shouldSkip(null, BlockPos.ZERO, lava)).isTrue();
        }

        @Test
        @DisplayName("mines a waterlogged block rather than leaving it standing")
        void minesWaterloggedBlocks() {
            for (BlockState state : waterlogged()) {
                assertThat(GridScanner.shouldSkip(null, BlockPos.ZERO, state))
                        .as("%s holds water but is not water", state.getBlock())
                        .isFalse();
            }
        }

        @Test
        @DisplayName("still skips watery blocks that are not solid, so a regenerating one is no detour")
        void skipsNonSolidWateryBlocks() {
            for (BlockState state : wateryButNotSolid()) {
                assertThat(GridScanner.shouldSkip(null, BlockPos.ZERO, state))
                        .as("%s is part of the water, not a block to break", state.getBlock())
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("isHazardousFluid")
    class IsHazardousFluid {

        @Test
        @DisplayName("true for lava")
        void trueForLava() {
            assertThat(GridScanner.isHazardousFluid(Blocks.LAVA.defaultBlockState())).isTrue();
        }

        @Test
        @DisplayName("false for water")
        void falseForWater() {
            assertThat(GridScanner.isHazardousFluid(Blocks.WATER.defaultBlockState())).isFalse();
        }

        @Test
        @DisplayName("false for air and solid blocks")
        void falseForNonFluids() {
            assertThat(GridScanner.isHazardousFluid(Blocks.AIR.defaultBlockState())).isFalse();
            assertThat(GridScanner.isHazardousFluid(Blocks.STONE.defaultBlockState())).isFalse();
        }

        @Test
        @DisplayName("false for a block that merely holds a fluid")
        void falseForWaterloggedBlocks() {
            for (BlockState state : waterlogged()) {
                assertThat(GridScanner.isHazardousFluid(state))
                        .as("%s holds water but is not water", state.getBlock())
                        .isFalse();
            }
        }
    }

    /**
     * Watery blocks with no collision. A bubble column matters most: it regenerates from the magma
     * block below, and the quarry would mine it forever without ever advancing its cursor.
     */
    private static java.util.List<BlockState> wateryButNotSolid() {
        return java.util.stream.Stream
                .of(Blocks.BUBBLE_COLUMN, Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS)
                .map(block -> block.defaultBlockState())
                .toList();
    }

    /** The shapes players actually find submerged: shipwreck stairs and slabs, ruin walls, chests. */
    private static java.util.List<BlockState> waterlogged() {
        return java.util.stream.Stream
                .of(Blocks.OAK_STAIRS, Blocks.OAK_SLAB, Blocks.OAK_FENCE, Blocks.COBBLESTONE_WALL, Blocks.CHEST)
                .map(block -> block.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true))
                .toList();
    }
}
