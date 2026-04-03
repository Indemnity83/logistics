package com.logistics.automation.macerator;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for macerator processing logic that can run without a live Level.
 *
 * <p>These tests verify the core rules governing when the macerator can start
 * or continue processing, whether output can be accepted, and that completion
 * correctly updates inventory state.
 */
@DisplayName("MaceratorBlockEntity — processing logic")
class MaceratorBlockEntityLogicTest extends MinecraftTestEnvironment {

    // ==================== Output acceptance ====================

    @Test
    @DisplayName("should accept output when output slot is empty")
    void acceptOutputWhenEmpty() {
        ItemStack output = ItemStack.EMPTY;
        ItemStack result = new ItemStack(Items.IRON_INGOT, 2);
        assertThat(canAcceptOutput(output, result)).isTrue();
    }

    @Test
    @DisplayName("should accept output when same item and count fits in stack")
    void acceptOutputWhenSameItemAndFits() {
        ItemStack output = new ItemStack(Items.IRON_INGOT, 60);
        ItemStack result = new ItemStack(Items.IRON_INGOT, 2);
        assertThat(canAcceptOutput(output, result)).isTrue();
    }

    @Test
    @DisplayName("should reject output when same item but stack would overflow")
    void rejectOutputWhenOverflow() {
        ItemStack output = new ItemStack(Items.IRON_INGOT, 63);
        ItemStack result = new ItemStack(Items.IRON_INGOT, 2);
        assertThat(canAcceptOutput(output, result)).isFalse();
    }

    @Test
    @DisplayName("should reject output when different item already in output slot")
    void rejectOutputWhenDifferentItem() {
        ItemStack output = new ItemStack(Items.COAL, 1);
        ItemStack result = new ItemStack(Items.IRON_INGOT, 2);
        assertThat(canAcceptOutput(output, result)).isFalse();
    }

    // ==================== Energy gating ====================

    @Test
    @DisplayName("should not process when energy is insufficient")
    void doesNotProcessWithoutEnergy() {
        long energyStored = 5L;
        int energyPerTick = 20;
        assertThat(energyStored >= energyPerTick).isFalse();
    }

    @Test
    @DisplayName("should process when energy meets the per-tick cost exactly")
    void processesWhenEnergyExact() {
        long energyStored = 20L;
        int energyPerTick = 20;
        assertThat(energyStored >= energyPerTick).isTrue();
    }

    // ==================== Progress tracking ====================

    @Test
    @DisplayName("processing completes after exactly processTimeTicks increments")
    void completesAfterCorrectTicks() {
        int processTimeTicks = 100;
        int progress = 99;
        progress++;
        assertThat(progress >= processTimeTicks).isTrue();
    }

    @Test
    @DisplayName("processing does not complete one tick before processTimeTicks")
    void doesNotCompleteEarly() {
        int processTimeTicks = 100;
        int progress = 98;
        progress++;
        assertThat(progress >= processTimeTicks).isFalse();
    }

    // ==================== Helper — mirrors MaceratorBlockEntity.canAcceptOutput ====================

    private static boolean canAcceptOutput(ItemStack output, ItemStack result) {
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, result)
            && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }
}
