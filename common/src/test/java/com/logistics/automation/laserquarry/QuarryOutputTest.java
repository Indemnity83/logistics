package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.automation.laserquarry.entity.QuarryOutput;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryOutput.insertIntoSlot")
class QuarryOutputTest extends MinecraftTestEnvironment {

    @Test
    @DisplayName("fully inserts into an empty slot")
    void insertsIntoEmptySlot() {
        SimpleContainer container = new SimpleContainer(1);
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 5);

        ItemStack remainder = QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(remainder.isEmpty()).isTrue();
        assertThat(container.getItem(0).getItem()).isEqualTo(Items.IRON_INGOT);
        assertThat(container.getItem(0).getCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("marks the container changed when inserting into an empty slot")
    void marksChangedWhenInsertingIntoEmptySlot() {
        TrackingContainer container = new TrackingContainer(1);
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 5);

        QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(container.changedCount).isPositive();
    }

    @Test
    @DisplayName("merges into a matching slot up to the stack size")
    void mergesIntoMatchingSlot() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.IRON_INGOT, 60));
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 10);

        ItemStack remainder = QuarryOutput.insertIntoSlot(container, 0, stack);

        // 64 - 60 = 4 fit, 6 remain.
        assertThat(container.getItem(0).getCount()).isEqualTo(64);
        assertThat(remainder.getCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("marks the container changed when merging into a matching slot")
    void marksChangedWhenMergingIntoMatchingSlot() {
        TrackingContainer container = new TrackingContainer(1);
        container.setItem(0, new ItemStack(Items.IRON_INGOT, 60));
        container.changedCount = 0;
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 10);

        QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(container.changedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("does not merge into a slot holding a different item")
    void doesNotMergeDifferentItem() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.COAL, 1));
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 5);

        ItemStack remainder = QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(container.getItem(0).getItem()).isEqualTo(Items.COAL);
        assertThat(container.getItem(0).getCount()).isEqualTo(1);
        assertThat(remainder.getCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("inserts only up to the container's max stack size into an empty slot")
    void respectsContainerMaxStackSize() {
        SimpleContainer container = new SimpleContainer(1) {
            @Override
            public int getMaxStackSize() {
                return 16;
            }
        };
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 40);

        ItemStack remainder = QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(container.getItem(0).getCount()).isEqualTo(16);
        assertThat(remainder.getCount()).isEqualTo(24);
    }

    @Test
    @DisplayName("returns the original stack when the matching slot is already full")
    void noChangeWhenMatchingSlotFull() {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        ItemStack stack = new ItemStack(Items.IRON_INGOT, 5);

        ItemStack remainder = QuarryOutput.insertIntoSlot(container, 0, stack);

        assertThat(remainder.getCount()).isEqualTo(5);
        assertThat(container.getItem(0).getCount()).isEqualTo(64);
    }

    private static final class TrackingContainer extends SimpleContainer {
        private int changedCount = 0;

        private TrackingContainer(int size) {
            super(size);
        }

        @Override
        public void setChanged() {
            changedCount++;
            super.setChanged();
        }
    }
}
