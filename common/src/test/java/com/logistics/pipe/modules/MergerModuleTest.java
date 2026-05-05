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

@DisplayName("MergerModule")
class MergerModuleTest extends MinecraftTestEnvironment {

    private MergerModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new MergerModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== route — no direction configured ====================

    @Test
    @DisplayName("route returns DROP when no output direction is configured")
    void route_noOutputDirection_returnsDrop() {
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);

        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    @DisplayName("route returns DROP when output direction is not in options")
    void route_outputDirectionNotInOptions_returnsDrop() {
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        // SOUTH is configured but not in options
        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.EAST));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    @DisplayName("route returns DROP when options list is empty")
    void route_emptyOptions_returnsDrop() {
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        RoutePlan plan = module.route(ctx, item, List.of());

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    // ==================== route — direction configured and in options ====================

    @Test
    @DisplayName("route returns REROUTE to configured direction when it is in options")
    void route_outputInOptions_returnsReroute() {
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.1f, null);
        RoutePlan plan = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.SOUTH);
    }

    // ==================== canAcceptFrom ====================

    @Test
    @DisplayName("canAcceptFrom returns true when no output direction is configured")
    void canAcceptFrom_noOutputDirection_returnsTrue() {
        assertThat(module.canAcceptFrom(ctx, Direction.NORTH, ItemStack.EMPTY)).isTrue();
    }

    @Test
    @DisplayName("canAcceptFrom returns false when incoming direction is the output direction")
    void canAcceptFrom_incomingIsOutput_returnsFalse() {
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        // SOUTH is the output direction; receiving from SOUTH should be rejected
        assertThat(module.canAcceptFrom(ctx, Direction.SOUTH, ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("canAcceptFrom returns true for a direction other than the output direction")
    void canAcceptFrom_incomingIsNotOutput_returnsTrue() {
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        // NORTH is not the output — should be accepted
        assertThat(module.canAcceptFrom(ctx, Direction.NORTH, ItemStack.EMPTY)).isTrue();
    }

    // ==================== onConnectionsChanged ====================

    @Test
    @DisplayName("onConnectionsChanged selects the first connected direction as output")
    void onConnectionsChanged_setsFirstDirection() {
        // NORTH (ordinal 2) precedes SOUTH (ordinal 3) in Direction.values(), so NORTH is chosen first.
        access.setConnection(Direction.NORTH, PipeConnection.Type.PIPE);
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.NORTH, Direction.SOUTH));

        // NORTH was first in Direction.values() — it becomes the output
        assertThat(module.canAcceptFrom(ctx, Direction.NORTH, ItemStack.EMPTY)).isFalse();
        // SOUTH is not the output — incoming items from SOUTH are accepted
        assertThat(module.canAcceptFrom(ctx, Direction.SOUTH, ItemStack.EMPTY)).isTrue();
    }

    @Test
    @DisplayName("onConnectionsChanged with no connections clears the output direction")
    void onConnectionsChanged_noConnections_clearsOutput() {
        // First set a direction
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);
        module.onConnectionsChanged(ctx, List.of(Direction.SOUTH));

        // Now clear all connections
        access.setConnection(Direction.SOUTH, PipeConnection.Type.NONE);
        module.onConnectionsChanged(ctx, List.of());

        // No output configured → canAcceptFrom returns true for all directions
        assertThat(module.canAcceptFrom(ctx, Direction.SOUTH, ItemStack.EMPTY)).isTrue();
    }
}
