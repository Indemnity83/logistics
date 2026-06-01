package com.logistics.pipe.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.pipe.RoutePlan;
import com.logistics.core.lib.pipe.TravelingItem;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class PipeRoutingPlannerTest extends MinecraftTestEnvironment {
    @Test
    void defaultPlan_dropsWhenNoValidDirectionsExist() {
        RoutePlan plan = PipeRoutingPlanner.defaultPlan(List.of());

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    void defaultPlan_reroutesToValidDirections() {
        RoutePlan plan = PipeRoutingPlanner.defaultPlan(List.of(Direction.NORTH, Direction.EAST));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH, Direction.EAST);
    }

    @Test
    void normalize_convertsPassToDefaultPlan() {
        RoutePlan fallback = RoutePlan.reroute(Direction.SOUTH);

        RoutePlan plan = PipeRoutingPlanner.normalize(RoutePlan.pass(), fallback);

        assertThat(plan).isSameAs(fallback);
    }

    @Test
    void normalize_convertsEmptyRerouteToDrop() {
        RoutePlan plan = PipeRoutingPlanner.normalize(RoutePlan.reroute(List.of()), RoutePlan.reroute(Direction.NORTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
    }

    @Test
    void normalize_convertsEmptySplitToDiscard() {
        RoutePlan plan = PipeRoutingPlanner.normalize(RoutePlan.split(List.of()), RoutePlan.reroute(Direction.NORTH));

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DISCARD);
    }

    @Test
    void normalize_preservesNonEmptySplit() {
        RoutePlan split = RoutePlan.split(List.of(
                new TravelingItem(new ItemStack(Items.DIAMOND, 1), Direction.NORTH, 0.05f)));

        RoutePlan plan = PipeRoutingPlanner.normalize(split, RoutePlan.reroute(Direction.SOUTH));

        assertThat(plan).isSameAs(split);
    }

    @Test
    void chooseDirection_isDeterministicForSameInputs() {
        List<Direction> options = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH);

        Direction first = PipeRoutingPlanner.chooseDirection(BlockPos.ZERO, 123L, Direction.WEST, options);
        Direction second = PipeRoutingPlanner.chooseDirection(BlockPos.ZERO, 123L, Direction.WEST, options);

        assertThat(first).isEqualTo(second);
        assertThat(options).contains(first);
    }
}
