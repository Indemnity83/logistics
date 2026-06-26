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
}
