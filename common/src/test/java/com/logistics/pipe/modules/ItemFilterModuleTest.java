package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


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
