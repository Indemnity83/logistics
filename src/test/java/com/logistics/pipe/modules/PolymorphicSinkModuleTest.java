package com.logistics.pipe.modules;

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

@DisplayName("PolymorphicSinkModule")
class PolymorphicSinkModuleTest extends MinecraftTestEnvironment {

    private PolymorphicSinkModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new PolymorphicSinkModule(3);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== matchesFilter — no sink direction ====================

    @Test
    @DisplayName("matchesFilter returns false when no sink direction is configured")
    void matchesFilter_noSinkDirection_returnsFalse() {
        // No direction configured → matchesFilter returns false immediately without world access
        assertThat(module.matchesFilter(ctx, new ItemStack(Items.DIAMOND))).isFalse();
    }

    // ==================== acceptsItem — no sink direction ====================

    @Test
    @DisplayName("acceptsItem returns false when no sink direction is configured")
    void acceptsItem_noSinkDirection_returnsFalse() {
        assertThat(module.acceptsItem(ctx, new ItemStack(Items.DIAMOND))).isFalse();
    }

    // ==================== route — no sink direction ====================

    @Test
    @DisplayName("route returns PASS when no sink direction is configured")
    void route_noSinkDirection_returnsPass() {
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);

        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.PASS);
    }

    @Test
    @DisplayName("route returns PASS when options list does not contain sink direction")
    void route_sinkDirectionNotInOptions_returnsPass() {
        // Manually save the sink direction so we can test the options check without full world
        // DirectionSerializer stores as string value of 3DDataValue
        ctx.saveString(module, "sink_direction", String.valueOf(Direction.SOUTH.get3DDataValue()));

        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        // SOUTH is configured but not in options → PASS (world never accessed)
        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.EAST));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.PASS);
    }
}
