package com.logistics.forestry.farm;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.forestry.farm.ColumnClearer.Clearing;
import com.logistics.forestry.farm.ColumnClearer.Kind;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Column clearing")
class ColumnClearerTest extends MinecraftTestEnvironment {

    private static final BlockPos PLOT = new BlockPos(4, 70, -2);

    /** Empty unless set. */
    private final Map<BlockPos, Kind> column = new HashMap<>();

    private void set(int dy, Kind kind) {
        column.put(PLOT.above(dy), kind);
    }

    private Clearing next(int height) {
        return ColumnClearer.next(pos -> column.getOrDefault(pos, Kind.EMPTY), PLOT, height);
    }

    // ==================== Planning ====================

    @Test
    @DisplayName("an empty column is clear")
    void emptyColumnIsClear() {
        assertThat(next(5)).isEqualTo(Clearing.CLEAR);
    }

    @Test
    @DisplayName("the topmost block that is not the farm's own goes first, so clearing runs top-down")
    void topmostFirst() {
        set(0, Kind.BREAKABLE);
        set(2, Kind.BREAKABLE);

        assertThat(next(5)).isEqualTo(new Clearing.Break(PLOT.above(2)));

        set(2, Kind.EMPTY);
        assertThat(next(5)).isEqualTo(new Clearing.Break(PLOT));
    }

    @Test
    @DisplayName("own growth is passed over: above it and below it, other blocks are still cleared")
    void skipsOwnGrowth() {
        set(0, Kind.OWN_GROWTH);
        set(1, Kind.OWN_GROWTH);
        assertThat(next(5)).isEqualTo(Clearing.CLEAR);

        set(3, Kind.BREAKABLE);
        assertThat(next(5)).isEqualTo(new Clearing.Break(PLOT.above(3)));

        set(3, Kind.EMPTY);
        set(4, Kind.OWN_GROWTH);
        set(2, Kind.BREAKABLE);
        assertThat(next(5)).isEqualTo(new Clearing.Break(PLOT.above(2)));
    }

    @Test
    @DisplayName("a protected block on top blocks the column; one lower down waits behind the blocks above it")
    void protectedBlocks() {
        set(1, Kind.PROTECTED);
        assertThat(next(5)).isEqualTo(new Clearing.Blocked(PLOT.above(1)));

        set(3, Kind.BREAKABLE);
        assertThat(next(5)).isEqualTo(new Clearing.Break(PLOT.above(3)));
    }

    @Test
    @DisplayName("a fluid is drained rather than broken")
    void drainsFluid() {
        set(0, Kind.FLUID);

        assertThat(next(5)).isEqualTo(new Clearing.Drain(PLOT));
    }

    @Test
    @DisplayName("only the column's height is looked at")
    void respectsHeight() {
        set(5, Kind.BREAKABLE);
        set(6, Kind.PROTECTED);

        assertThat(next(5)).isEqualTo(Clearing.CLEAR);
        assertThat(next(6)).isEqualTo(new Clearing.Break(PLOT.above(5)));
    }

    // ==================== Classification ====================

    @Test
    @DisplayName("air is empty and the farm's own growth is its own, whatever the block")
    void classifiesEmptyAndOwn() {
        assertThat(ColumnClearer.classify(Blocks.AIR.defaultBlockState(), 0, false)).isEqualTo(Kind.EMPTY);
        assertThat(ColumnClearer.classify(Blocks.AIR.defaultBlockState(), 0, true)).isEqualTo(Kind.EMPTY);
        assertThat(ColumnClearer.classify(Blocks.WHEAT.defaultBlockState(), 0, true)).isEqualTo(Kind.OWN_GROWTH);
        assertThat(ColumnClearer.classify(Blocks.WHEAT.defaultBlockState(), 0, false)).isEqualTo(Kind.BREAKABLE);
    }

    @Test
    @DisplayName("block entities and unbreakable blocks are protected")
    void classifiesProtected() {
        assertThat(ColumnClearer.classify(Blocks.CHEST.defaultBlockState(), 2.5f, false)).isEqualTo(Kind.PROTECTED);
        assertThat(ColumnClearer.classify(Blocks.HOPPER.defaultBlockState(), 3f, false)).isEqualTo(Kind.PROTECTED);
        assertThat(ColumnClearer.classify(Blocks.BEDROCK.defaultBlockState(), -1f, false)).isEqualTo(Kind.PROTECTED);
    }

    @Test
    @DisplayName("water and lava, source or flowing, are fluids; a waterlogged block is a block")
    void classifiesFluids() {
        assertThat(ColumnClearer.classify(Blocks.WATER.defaultBlockState(), 100f, false)).isEqualTo(Kind.FLUID);
        assertThat(ColumnClearer.classify(
                        Fluids.FLOWING_WATER.getFlowing(3, false).createLegacyBlock(), 100f, false))
                .isEqualTo(Kind.FLUID);
        assertThat(ColumnClearer.classify(Blocks.LAVA.defaultBlockState(), 100f, false)).isEqualTo(Kind.FLUID);
        assertThat(ColumnClearer.classify(
                        Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                        2f,
                        false))
                .isEqualTo(Kind.BREAKABLE);
    }

    @Test
    @DisplayName("anything else is breakable: stone, a torch, sand, grass")
    void classifiesBreakable() {
        assertThat(ColumnClearer.classify(Blocks.COBBLESTONE.defaultBlockState(), 2f, false)).isEqualTo(Kind.BREAKABLE);
        assertThat(ColumnClearer.classify(Blocks.TORCH.defaultBlockState(), 0f, false)).isEqualTo(Kind.BREAKABLE);
        assertThat(ColumnClearer.classify(Blocks.SAND.defaultBlockState(), 0.5f, false)).isEqualTo(Kind.BREAKABLE);
        assertThat(ColumnClearer.classify(Blocks.SHORT_GRASS.defaultBlockState(), 0f, false)).isEqualTo(Kind.BREAKABLE);
    }
}
