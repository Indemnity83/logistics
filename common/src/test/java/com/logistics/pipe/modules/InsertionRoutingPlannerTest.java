package com.logistics.pipe.modules;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class InsertionRoutingPlannerTest extends MinecraftTestEnvironment {
    @Test
    void route_prefersInventoryWithEnoughSpaceOverPipes() {
        RoutePlan plan = InsertionRoutingPlanner.route(
                item(8),
                List.of(
                        new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 8),
                        new InsertionRoutingPlanner.InventoryOption(Direction.SOUTH, 4)),
                List.of(Direction.EAST),
                1L);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH);
    }

    @Test
    void route_splitsPartialInventorySpaceAndRemainingAmountToPipe() {
        RoutePlan plan = InsertionRoutingPlanner.route(
                item(8),
                List.of(
                        new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 3),
                        new InsertionRoutingPlanner.InventoryOption(Direction.SOUTH, 2)),
                List.of(Direction.EAST),
                1L);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.SPLIT);
        assertThat(plan.getItems()).hasSize(3);
        assertThat(plan.getItems().get(0).getDirection()).isEqualTo(Direction.NORTH);
        assertThat(plan.getItems().get(0).getStack().getCount()).isEqualTo(3);
        assertThat(plan.getItems().get(1).getDirection()).isEqualTo(Direction.SOUTH);
        assertThat(plan.getItems().get(1).getStack().getCount()).isEqualTo(2);
        assertThat(plan.getItems().get(2).getDirection()).isEqualTo(Direction.EAST);
        assertThat(plan.getItems().get(2).getStack().getCount()).isEqualTo(3);
    }

    @Test
    void route_reroutesToPartialInventoriesWhenNoPipeFallbackExists() {
        RoutePlan plan = InsertionRoutingPlanner.route(
                item(8),
                List.of(
                        new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 3),
                        new InsertionRoutingPlanner.InventoryOption(Direction.SOUTH, 2)),
                List.of(),
                1L);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH, Direction.SOUTH);
    }

    @Test
    void route_reroutesToPipesWhenNoInventoryHasSpace() {
        RoutePlan plan = InsertionRoutingPlanner.route(
                item(8),
                List.of(new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 0)),
                List.of(Direction.EAST, Direction.WEST),
                1L);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.EAST, Direction.WEST);
    }

    @Test
    void route_dropsWhenNoInventoryOrPipeCanAccept() {
        RoutePlan plan = InsertionRoutingPlanner.route(
                item(8),
                List.of(new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 0)),
                List.of(),
                1L);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    void route_usesDeterministicPipeFallbackWhenSplitting() {
        TravelingItem item = item(8);
        List<InsertionRoutingPlanner.InventoryOption> inventories = List.of(
                new InsertionRoutingPlanner.InventoryOption(Direction.NORTH, 3));
        List<Direction> pipes = List.of(Direction.EAST, Direction.WEST, Direction.SOUTH);
        long seed = InsertionRoutingPlanner.routeSeed(42L, 99L, Direction.UP);

        RoutePlan first = InsertionRoutingPlanner.route(item, inventories, pipes, seed);
        RoutePlan second = InsertionRoutingPlanner.route(item, inventories, pipes, seed);

        assertThat(first.getType()).isEqualTo(RoutePlan.Type.SPLIT);
        assertThat(second.getType()).isEqualTo(RoutePlan.Type.SPLIT);
        assertThat(first.getItems().get(1).getDirection()).isEqualTo(second.getItems().get(1).getDirection());
    }

    private static TravelingItem item(int count) {
        return new TravelingItem(new ItemStack(Items.DIAMOND, count), Direction.NORTH, 0.05f);
    }
}
