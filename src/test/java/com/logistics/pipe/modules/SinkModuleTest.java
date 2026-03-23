package com.logistics.pipe.modules;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.FakePipeAccess;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SinkModule")
class SinkModuleTest extends MinecraftTestEnvironment {

    private SinkModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new SinkModule(10);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Default route flag ====================

    @Test
    @DisplayName("isDefaultRoute is false when no state has been written")
    void isDefaultRoute_falseByDefault() {
        assertThat(module.isDefaultRoute(ctx)).isFalse();
    }

    @Test
    @DisplayName("isDefaultRoute reads DEFAULT_ROUTE int from NBT (1 = true)")
    void isDefaultRoute_trueWhenStateIsOne() {
        ctx.saveInt(module, SinkModule.DEFAULT_ROUTE, 1);
        assertThat(module.isDefaultRoute(ctx)).isTrue();
    }

    @Test
    @DisplayName("isDefaultRoute is false when DEFAULT_ROUTE is 0")
    void isDefaultRoute_falseWhenStateIsZero() {
        ctx.saveInt(module, SinkModule.DEFAULT_ROUTE, 0);
        assertThat(module.isDefaultRoute(ctx)).isFalse();
    }

    // ==================== Filter slot management ====================

    @Test
    @DisplayName("getFilters returns empty FilterSlots when nothing is configured")
    void getFilters_emptyByDefault() {
        assertThat(module.getFilters(ctx).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("setFilter persists the item ID and can be read back")
    void setFilter_persistsAndReadsBack() {
        module.setFilter(ctx, 0, "minecraft:diamond");

        var filters = module.getFilters(ctx);
        assertThat(filters.asList()).contains("minecraft:diamond");
    }

    @Test
    @DisplayName("setFilter in different slots are independent")
    void setFilter_multipleSlotsAreIndependent() {
        module.setFilter(ctx, 0, "minecraft:diamond");
        module.setFilter(ctx, 3, "minecraft:iron_ingot");

        var filters = module.getFilters(ctx);
        assertThat(filters.asList()).contains("minecraft:diamond", "minecraft:iron_ingot");
    }

    @Test
    @DisplayName("setFilter with empty string clears the slot")
    void setFilter_emptyStringClearsSlot() {
        module.setFilter(ctx, 0, "minecraft:diamond");
        module.setFilter(ctx, 0, "");

        var filters = module.getFilters(ctx);
        assertThat(filters.asList().stream().filter(s -> !s.isEmpty())).isEmpty();
    }

    @Test
    @DisplayName("setFilter throws IllegalArgumentException for negative slot index")
    void setFilter_rejectsNegativeSlot() {
        assertThatThrownBy(() -> module.setFilter(ctx, -1, "minecraft:diamond"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setFilter throws IllegalArgumentException for slot >= MAX_FILTER_SLOTS")
    void setFilter_rejectsSlotAtOrBeyondMax() {
        assertThatThrownBy(() -> module.setFilter(ctx, SinkModule.MAX_FILTER_SLOTS, "minecraft:diamond"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("MAX_FILTER_SLOTS is 9")
    void maxFilterSlots_isNine() {
        assertThat(SinkModule.MAX_FILTER_SLOTS).isEqualTo(9);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments the tick counter")
    void onTick_incrementsCounter() {
        module.onTick(ctx);
        module.onTick(ctx);

        // After 2 ticks (well below SYNC_INTERVAL=20), counter should be 2
        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(2);
    }

    @Test
    @DisplayName("onTick resets counter after SYNC_INTERVAL ticks when network is null")
    void onTick_resetsCounterAfterSyncInterval_withNullNetwork() {
        // 20 ticks triggers sync but network is null so it's a no-op — counter resets
        for (int i = 0; i < 20; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(0);
    }

    // ==================== Routing ====================

    @Test
    @DisplayName("route returns pass when no sink direction is configured")
    void route_noSinkDirection_returnsPass() {
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        List<Direction> options = List.of(Direction.NORTH, Direction.SOUTH);

        RoutePlan plan = module.route(ctx, item, options);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.PASS);
    }

    @Test
    @DisplayName("route returns pass when sink direction is not in options")
    void route_sinkDirectionNotInOptions_returnsPass() {
        // Configure SOUTH as a sink direction by faking a connection then calling onConnectionsChanged
        access.setConnection(Direction.SOUTH, PipeConnection.Type.INVENTORY);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        // Now route with options that don't include SOUTH (no world access needed for this path)
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH)); // SOUTH not in options

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.PASS);
    }

    // ==================== acceptsItem ====================

    @Test
    @DisplayName("acceptsItem returns false when no filters and not default route")
    void acceptsItem_noFiltersNoDefaultRoute_returnsFalse() {
        // No filter configured, no default route → acceptsItem calls matchesFilter (empty = no match)
        // and isDefaultRoute (false). Result: false.
        // matchesFilter loops over getFilters which returns empty, so no BuiltInRegistries access.
        assertThat(module.acceptsItem(ctx, new ItemStack(Items.DIAMOND))).isFalse();
    }

    @Test
    @DisplayName("acceptsItem returns true when default route is enabled")
    void acceptsItem_defaultRouteEnabled_returnsTrue() {
        ctx.saveInt(module, SinkModule.DEFAULT_ROUTE, 1);
        assertThat(module.acceptsItem(ctx, new ItemStack(Items.DIAMOND))).isTrue();
    }
}
