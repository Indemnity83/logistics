package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ItemFilterModule")
class ItemFilterModuleTest extends MinecraftTestEnvironment {

    private ItemFilterModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ItemFilterModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    private RoutePlan route(Item item, Direction... options) {
        TravelingItem traveling = new TravelingItem(new ItemStack(item), Direction.SOUTH, 0.1f);
        return module.route(ctx, traveling, List.of(options));
    }

    // ==================== route ====================
    //
    // Only the world-independent half of route() lives here. Reading a configured filter goes
    // through ItemStack.CODEC with the level's RegistryOps, so an unfiltered PipeContext is as far
    // as JUnit reaches; the filtered cases are covered end-to-end by ModuleGameTestBody's real
    // filter junction, which asserts the chest each item actually lands in.

    @Test
    @DisplayName("route offers every option when no side is filtered")
    void route_noFiltersConfigured_reroutesToAllOptions() {
        RoutePlan plan = route(Items.DIAMOND, Direction.NORTH, Direction.EAST);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactlyInAnyOrder(Direction.NORTH, Direction.EAST);
    }

    @Test
    @DisplayName("route offers no direction when there are no options")
    void route_noOptions_reroutesNowhere() {
        RoutePlan plan = route(Items.DIAMOND);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).isEmpty();
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
