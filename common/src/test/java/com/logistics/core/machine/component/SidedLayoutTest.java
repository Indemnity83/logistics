package com.logistics.core.machine.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class SidedLayoutTest extends MinecraftTestEnvironment {

    private static final Predicate<ItemStack> ANY = stack -> true;
    private static final ItemStack STACK = new ItemStack(Items.RAW_IRON);

    // Slot 0 is the input; slots 1 and 2 are outputs.
    private static SidedLayout furnace() {
        return SidedLayout.furnace(new int[] {0}, new int[] {1, 2}, ANY);
    }

    @Test
    void furnaceExposesInputsUpAndOutputsDownOnly() {
        SidedLayout layout = furnace();

        assertThat(layout.slotsForFace(Direction.UP)).containsExactly(0);
        assertThat(layout.slotsForFace(Direction.DOWN)).containsExactly(1, 2);
        // Horizontal faces expose nothing on a furnace.
        assertThat(layout.slotsForFace(Direction.NORTH)).isEmpty();
        assertThat(layout.slotsForFace(Direction.EAST)).isEmpty();
    }

    @Test
    void furnaceAcceptsInputFromTheTopOnly() {
        SidedLayout layout = furnace();

        assertThat(layout.canPlace(0, STACK, Direction.UP)).isTrue();
        // A side exposes no slots, so the input is unreachable from it.
        assertThat(layout.canPlace(0, STACK, Direction.NORTH)).isFalse();
    }

    @Test
    void furnaceRejectsInsertionIntoAnOutputSlot() {
        SidedLayout layout = furnace();

        // Slot 1 is an output; even from the face that exposes it, it is not insertable.
        assertThat(layout.canPlace(1, STACK, Direction.DOWN)).isFalse();
    }

    @Test
    void furnaceExtractsOutputsFromTheBottomOnly() {
        SidedLayout layout = furnace();

        assertThat(layout.canTake(1, STACK, Direction.DOWN)).isTrue();
        assertThat(layout.canTake(2, STACK, Direction.DOWN)).isTrue();
        // The top exposes the input, not the outputs.
        assertThat(layout.canTake(1, STACK, Direction.UP)).isFalse();
        // The input slot is never extractable.
        assertThat(layout.canTake(0, STACK, Direction.DOWN)).isFalse();
    }

    @Test
    void insertFilterGatesPlacement() {
        SidedLayout ironOnly = SidedLayout.furnace(new int[] {0}, new int[] {1}, stack -> stack.is(Items.RAW_IRON));

        assertThat(ironOnly.canPlace(0, new ItemStack(Items.RAW_IRON), Direction.UP)).isTrue();
        assertThat(ironOnly.canPlace(0, new ItemStack(Items.RAW_COPPER), Direction.UP)).isFalse();
    }

    @Test
    void bottomOutExposesInputsOnHorizontalFacesToo() {
        SidedLayout layout = SidedLayout.bottomOut(new int[] {0}, new int[] {1, 2}, ANY);

        assertThat(layout.slotsForFace(Direction.NORTH)).containsExactly(0);
        assertThat(layout.canPlace(0, STACK, Direction.NORTH)).isTrue();
        assertThat(layout.canPlace(0, STACK, Direction.UP)).isTrue();
        // Outputs still extract only from the bottom.
        assertThat(layout.canTake(1, STACK, Direction.DOWN)).isTrue();
        assertThat(layout.canTake(1, STACK, Direction.NORTH)).isFalse();
    }

    // Slots 0-1 are inputs, 2-3 outputs, 4 belongs to neither (e.g. an upgrade slot).
    private static SidedLayout anyFace() {
        return SidedLayout.anyFace(new int[] {0, 1}, new int[] {2, 3});
    }

    @Test
    void anyFaceExposesInputsAndOutputsOnEveryFace() {
        SidedLayout layout = anyFace();

        for (Direction face : Direction.values()) {
            assertThat(layout.slotsForFace(face)).containsExactly(0, 1, 2, 3);
            assertThat(layout.canPlace(0, STACK, face)).isTrue();
            assertThat(layout.canPlace(1, STACK, face)).isTrue();
            assertThat(layout.canTake(2, STACK, face)).isTrue();
            assertThat(layout.canTake(3, STACK, face)).isTrue();
        }
    }

    @Test
    void anyFaceExtractsOnlyOutputsAndInsertsOnlyInputs() {
        SidedLayout layout = anyFace();

        for (Direction face : Direction.values()) {
            assertThat(layout.canTake(0, STACK, face)).isFalse();
            assertThat(layout.canTake(1, STACK, face)).isFalse();
            assertThat(layout.canPlace(2, STACK, face)).isFalse();
            assertThat(layout.canPlace(3, STACK, face)).isFalse();
        }
    }

    @Test
    void anyFaceLeavesSlotsOutsideBothArraysUnreachable() {
        SidedLayout layout = anyFace();

        for (Direction face : Direction.values()) {
            assertThat(layout.slotsForFace(face)).doesNotContain(4);
            assertThat(layout.canPlace(4, STACK, face)).isFalse();
            assertThat(layout.canTake(4, STACK, face)).isFalse();
        }
    }

    @Test
    void slotFilterGatesPlacementPerSlot() {
        SidedLayout layout = anyFace()
                .withSlotFilter((slot, stack) -> slot == 0 ? stack.is(Items.WHEAT_SEEDS) : stack.is(Items.BONE_MEAL));
        ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS);
        ItemStack boneMeal = new ItemStack(Items.BONE_MEAL);

        assertThat(layout.canPlace(0, seeds, Direction.NORTH)).isTrue();
        assertThat(layout.canPlace(0, boneMeal, Direction.NORTH)).isFalse();
        assertThat(layout.canPlace(1, boneMeal, Direction.DOWN)).isTrue();
        assertThat(layout.canPlace(1, seeds, Direction.DOWN)).isFalse();
        assertThat(layout.accepts(0, seeds)).isTrue();
        assertThat(layout.accepts(0, boneMeal)).isFalse();
    }

    @Test
    void slotFilterDoesNotChangeExtraction() {
        SidedLayout layout = anyFace().withSlotFilter((slot, stack) -> false);

        assertThat(layout.canTake(2, STACK, Direction.UP)).isTrue();
        assertThat(layout.canTake(0, STACK, Direction.UP)).isFalse();
    }

    @Test
    void existingLayoutsAcceptEverySlotByDefault() {
        for (SidedLayout layout : new SidedLayout[] {
            furnace(),
            SidedLayout.bottomOut(new int[] {0}, new int[] {1, 2}, ANY),
            SidedLayout.topIn(new int[] {0}, ANY)
        }) {
            for (int slot = 0; slot < 3; slot++) {
                assertThat(layout.accepts(slot, STACK)).isTrue();
            }
        }
    }

    @Test
    void topInExposesInputsUpAndNothingExtractable() {
        SidedLayout layout = SidedLayout.topIn(new int[] {0}, ANY);

        assertThat(layout.slotsForFace(Direction.UP)).containsExactly(0);
        assertThat(layout.slotsForFace(Direction.DOWN)).isEmpty();
        assertThat(layout.slotsForFace(Direction.NORTH)).isEmpty();
        assertThat(layout.canPlace(0, STACK, Direction.UP)).isTrue();
        assertThat(layout.canTake(0, STACK, Direction.UP)).isFalse();
    }
}
