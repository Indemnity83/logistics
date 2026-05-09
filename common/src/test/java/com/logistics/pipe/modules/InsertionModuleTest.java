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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InsertionModule} routing decisions.
 *
 * <p>Uses a null world ({@code PipeContext(null, ...)}) which is safe for the tested paths:
 * {@link PipeContext#isInventoryConnection} and {@link PipeContext#isNeighborPipe} both
 * delegate to {@link FakePipeAccess} and never dereference the world.
 *
 * <p>Inventory routing paths (full/partial space, split) cannot be tested without a real
 * {@code Level} — {@code ItemStorageLookup.find(level, ...)} NPEs with null world.
 * See TESTING.md for details.
 */
@DisplayName("InsertionModule")
class InsertionModuleTest extends MinecraftTestEnvironment {

    private InsertionModule module;
    private FakePipeAccess access;
    private PipeContext ctx;
    private TravelingItem item;

    @BeforeEach
    void setUp() {
        module = new InsertionModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
        item = new TravelingItem(new ItemStack(Items.DIAMOND, 4), Direction.NORTH, 0.05f);
    }

    @Test
    @DisplayName("null options → drop")
    void nullOptions_drops() {
        RoutePlan result = module.route(ctx, item, null);
        assertThat(result.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    @DisplayName("empty options → drop")
    void emptyOptions_drops() {
        RoutePlan result = module.route(ctx, item, List.of());
        assertThat(result.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    @DisplayName("pipe-only connections → reroute to available pipe directions")
    void pipeOnly_reroutesToPipes() {
        access.setConnection(Direction.NORTH, PipeConnection.Type.PIPE);
        access.setConnection(Direction.SOUTH, PipeConnection.Type.PIPE);

        RoutePlan result = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(result.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(result.getDirections()).containsExactlyInAnyOrder(Direction.NORTH, Direction.SOUTH);
    }

    @Test
    @DisplayName("no connections in options → drop")
    void noConnections_drops() {
        // All connections remain NONE (FakePipeAccess default)
        RoutePlan result = module.route(ctx, item, List.of(Direction.NORTH, Direction.SOUTH));

        assertThat(result.getType()).isEqualTo(RoutePlan.Type.DROP);
    }
}
