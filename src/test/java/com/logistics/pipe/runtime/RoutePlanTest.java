package com.logistics.pipe.runtime;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("RoutePlan")
class RoutePlanTest extends MinecraftTestEnvironment {

    // ==================== Singleton Plans Tests ====================

    @Test
    @DisplayName("should create immutable PASS plan")
    void passImmutable() {
        RoutePlan plan = RoutePlan.pass();

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.PASS);
        assertThat(plan.getDirections()).isEmpty();
        assertThat(plan.getDirections()).isUnmodifiable();
        assertThat(plan.getItems()).isEmpty();
        assertThat(plan.getItems()).isUnmodifiable();
    }

    @Test
    @DisplayName("should create immutable DROP plan")
    void dropImmutable() {
        RoutePlan plan = RoutePlan.drop();

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DROP);
        assertThat(plan.getDirections()).isEmpty();
        assertThat(plan.getDirections()).isUnmodifiable();
        assertThat(plan.getItems()).isEmpty();
        assertThat(plan.getItems()).isUnmodifiable();
    }

    @Test
    @DisplayName("should create immutable DISCARD plan")
    void discardImmutable() {
        RoutePlan plan = RoutePlan.discard();

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.DISCARD);
        assertThat(plan.getDirections()).isEmpty();
        assertThat(plan.getDirections()).isUnmodifiable();
        assertThat(plan.getItems()).isEmpty();
        assertThat(plan.getItems()).isUnmodifiable();
    }

    @Test
    @DisplayName("should return same instance for singleton plans")
    void singletonIdentity() {
        RoutePlan pass1 = RoutePlan.pass();
        RoutePlan pass2 = RoutePlan.pass();

        assertThat(pass1).isSameAs(pass2);

        RoutePlan drop1 = RoutePlan.drop();
        RoutePlan drop2 = RoutePlan.drop();

        assertThat(drop1).isSameAs(drop2);

        RoutePlan discard1 = RoutePlan.discard();
        RoutePlan discard2 = RoutePlan.discard();

        assertThat(discard1).isSameAs(discard2);
    }

    // ==================== REROUTE Single Direction Tests ====================

    @Test
    @DisplayName("should create REROUTE plan with single direction")
    void rerouteSingleDirection() {
        RoutePlan plan = RoutePlan.reroute(Direction.NORTH);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH);
        assertThat(plan.getItems()).isEmpty();
    }

    @Test
    @DisplayName("should create REROUTE plan with different directions")
    void rerouteDifferentDirections() {
        RoutePlan north = RoutePlan.reroute(Direction.NORTH);
        RoutePlan south = RoutePlan.reroute(Direction.SOUTH);

        assertThat(north.getDirections()).containsExactly(Direction.NORTH);
        assertThat(south.getDirections()).containsExactly(Direction.SOUTH);
    }

    // ==================== REROUTE Multiple Directions Tests ====================

    @Test
    @DisplayName("should create REROUTE plan with multiple directions")
    void rerouteMultipleDirections() {
        List<Direction> directions = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH);
        RoutePlan plan = RoutePlan.reroute(directions);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH, Direction.EAST, Direction.SOUTH);
        assertThat(plan.getItems()).isEmpty();
    }

    @Test
    @DisplayName("should defensively copy direction list")
    void rerouteDefensiveCopy() {
        List<Direction> original = new ArrayList<>();
        original.add(Direction.NORTH);
        original.add(Direction.SOUTH);

        RoutePlan plan = RoutePlan.reroute(original);

        // Modify original list
        original.add(Direction.EAST);
        original.clear();

        // Plan should be unaffected
        assertThat(plan.getDirections()).containsExactly(Direction.NORTH, Direction.SOUTH);
    }

    @Test
    @DisplayName("should return immutable direction list")
    void rerouteImmutableList() {
        RoutePlan plan = RoutePlan.reroute(List.of(Direction.NORTH, Direction.SOUTH));
        List<Direction> directions = plan.getDirections();

        // Should throw when attempting to modify
        assertThat(directions)
                .isUnmodifiable()
                .containsExactly(Direction.NORTH, Direction.SOUTH);
    }

    // ==================== SPLIT Tests ====================

    @Test
    @DisplayName("should create SPLIT plan with items list")
    void splitWithItems() {
        TravelingItem item1 = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.05f);
        TravelingItem item2 = new TravelingItem(new ItemStack(Items.GOLD_INGOT), Direction.SOUTH, 0.05f);

        List<TravelingItem> items = List.of(item1, item2);
        RoutePlan plan = RoutePlan.split(items);

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.SPLIT);
        assertThat(plan.getItems()).hasSize(2);
        assertThat(plan.getItems()).containsExactly(item1, item2);
        assertThat(plan.getDirections()).isEmpty();
    }

    @Test
    @DisplayName("should create SPLIT plan with empty items list")
    void splitWithEmptyList() {
        RoutePlan plan = RoutePlan.split(List.of());

        assertThat(plan.getType()).isEqualTo(RoutePlan.Type.SPLIT);
        assertThat(plan.getItems()).isEmpty();
    }

    @Test
    @DisplayName("should preserve item details in SPLIT plan")
    void splitPreservesItemDetails() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 5);
        TravelingItem item = new TravelingItem(stack, Direction.NORTH, 0.05f);

        RoutePlan plan = RoutePlan.split(List.of(item));

        TravelingItem retrieved = plan.getItems().get(0);
        assertThat(retrieved.getStack().getItem()).isEqualTo(Items.DIAMOND);
        assertThat(retrieved.getStack().getCount()).isEqualTo(5);
        assertThat(retrieved.getDirection()).isEqualTo(Direction.NORTH);
        assertThat(retrieved.getSpeed()).isCloseTo(0.05f, within(0.0001f));
    }

    @Test
    @DisplayName("should defensively copy items list")
    void splitDefensiveCopy() {
        TravelingItem item1 = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.05f);
        TravelingItem item2 = new TravelingItem(new ItemStack(Items.GOLD_INGOT), Direction.SOUTH, 0.05f);

        List<TravelingItem> original = new ArrayList<>();
        original.add(item1);
        original.add(item2);

        RoutePlan plan = RoutePlan.split(original);

        // Modify original list
        original.add(new TravelingItem(new ItemStack(Items.IRON_INGOT), Direction.EAST, 0.05f));
        original.clear();

        // Plan should be unaffected
        assertThat(plan.getItems()).containsExactly(item1, item2);
    }

    // ==================== Type Verification Tests ====================

    @Test
    @DisplayName("should have distinct types for each plan variant")
    void distinctTypes() {
        assertThat(RoutePlan.pass().getType()).isEqualTo(RoutePlan.Type.PASS);
        assertThat(RoutePlan.drop().getType()).isEqualTo(RoutePlan.Type.DROP);
        assertThat(RoutePlan.discard().getType()).isEqualTo(RoutePlan.Type.DISCARD);
        assertThat(RoutePlan.reroute(Direction.NORTH).getType()).isEqualTo(RoutePlan.Type.REROUTE);
        assertThat(RoutePlan.split(List.of()).getType()).isEqualTo(RoutePlan.Type.SPLIT);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("should handle all six directions in REROUTE")
    void rerouteAllDirections() {
        List<Direction> allDirections = List.of(
                Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST,
                Direction.UP, Direction.DOWN
        );

        RoutePlan plan = RoutePlan.reroute(allDirections);

        assertThat(plan.getDirections()).containsExactlyElementsOf(allDirections);
    }

    @Test
    @DisplayName("should handle single item in SPLIT")
    void splitSingleItem() {
        TravelingItem item = new TravelingItem(new ItemStack(Items.DIAMOND), Direction.NORTH, 0.05f);
        RoutePlan plan = RoutePlan.split(List.of(item));

        assertThat(plan.getItems()).hasSize(1);
        assertThat(plan.getItems().get(0)).isSameAs(item);
    }
}
