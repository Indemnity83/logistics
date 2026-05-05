package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItemFilterModule")
class ItemFilterModuleTest {

    private ItemFilterModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ItemFilterModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("FILTERS constant is 'filters'")
    void filtersKey_value() {
        assertThat(ItemFilterModule.FILTERS).isEqualTo("filters");
    }

    @Test
    @DisplayName("FILTER_SLOTS_PER_SIDE is 8")
    void filterSlotsPerSide_isEight() {
        assertThat(ItemFilterModule.FILTER_SLOTS_PER_SIDE).isEqualTo(8);
    }

    @Test
    @DisplayName("FILTER_ORDER contains all 6 directions")
    void filterOrder_containsAllDirections() {
        assertThat(ItemFilterModule.FILTER_ORDER).hasSize(6);
        assertThat(ItemFilterModule.FILTER_ORDER).contains(
                Direction.NORTH, Direction.SOUTH, Direction.WEST,
                Direction.EAST, Direction.UP, Direction.DOWN);
    }

    // ==================== getFilterSlots — defaults ====================

    @ParameterizedTest
    @DisplayName("getFilterSlots returns FILTER_SLOTS_PER_SIDE empty strings when nothing configured")
    @EnumSource(Direction.class)
    void getFilterSlots_emptyByDefault(Direction direction) {
        List<String> slots = module.getFilterSlots(ctx, direction);
        assertThat(slots).hasSize(ItemFilterModule.FILTER_SLOTS_PER_SIDE);
        assertThat(slots).allMatch(String::isEmpty);
    }

    // ==================== setFilterSlots / getFilterSlots round-trip ====================

    @Test
    @DisplayName("setFilterSlots persists items for the given direction")
    void setFilterSlots_persistsAndReadsBack() {
        List<String> slots = List.of("minecraft:diamond", "minecraft:iron_ingot", "", "", "", "", "", "");
        module.setFilterSlots(ctx, Direction.NORTH, slots);

        List<String> result = module.getFilterSlots(ctx, Direction.NORTH);
        assertThat(result.get(0)).isEqualTo("minecraft:diamond");
        assertThat(result.get(1)).isEqualTo("minecraft:iron_ingot");
        assertThat(result.get(2)).isEmpty();
    }

    @Test
    @DisplayName("setFilterSlots for different directions are independent")
    void setFilterSlots_differentDirectionsAreIndependent() {
        List<String> northSlots = List.of("minecraft:diamond", "", "", "", "", "", "", "");
        List<String> southSlots = List.of("minecraft:iron_ingot", "", "", "", "", "", "", "");

        module.setFilterSlots(ctx, Direction.NORTH, northSlots);
        module.setFilterSlots(ctx, Direction.SOUTH, southSlots);

        assertThat(module.getFilterSlots(ctx, Direction.NORTH).get(0)).isEqualTo("minecraft:diamond");
        assertThat(module.getFilterSlots(ctx, Direction.SOUTH).get(0)).isEqualTo("minecraft:iron_ingot");
        assertThat(module.getFilterSlots(ctx, Direction.EAST).get(0)).isEmpty();
    }

    @Test
    @DisplayName("setFilterSlots with all empty strings clears the direction's filter")
    void setFilterSlots_allEmpty_clearsFilter() {
        List<String> slots = List.of("minecraft:diamond", "", "", "", "", "", "", "");
        module.setFilterSlots(ctx, Direction.NORTH, slots);

        List<String> empty = List.of("", "", "", "", "", "", "", "");
        module.setFilterSlots(ctx, Direction.NORTH, empty);

        assertThat(module.getFilterSlots(ctx, Direction.NORTH)).allMatch(String::isEmpty);
    }

    // ==================== getFilterColor ====================

    @Test
    @DisplayName("getFilterColor returns distinct colors for each direction")
    void getFilterColor_distinctPerDirection() {
        int north = ItemFilterModule.getFilterColor(Direction.NORTH);
        int south = ItemFilterModule.getFilterColor(Direction.SOUTH);
        int west  = ItemFilterModule.getFilterColor(Direction.WEST);
        int east  = ItemFilterModule.getFilterColor(Direction.EAST);
        int up    = ItemFilterModule.getFilterColor(Direction.UP);
        int down  = ItemFilterModule.getFilterColor(Direction.DOWN);

        assertThat(north).isNotEqualTo(south);
        assertThat(north).isNotEqualTo(up);
        // All six should be unique
        assertThat(java.util.Set.of(north, south, west, east, up, down)).hasSize(6);
    }
}
