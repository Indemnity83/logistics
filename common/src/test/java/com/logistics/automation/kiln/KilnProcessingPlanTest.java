package com.logistics.automation.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

class KilnProcessingPlanTest extends MinecraftTestEnvironment {
    @Test
    void advance_pausesWhenEnergyIsInsufficient() {
        KilnProcessingPlan.Result result = KilnProcessingPlan.advance(3, 10, 0, 1, true);

        assertThat(result.progress()).isEqualTo(3);
        assertThat(result.consumedEnergy()).isFalse();
        assertThat(result.lit()).isFalse();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void advance_pausesWhenOutputCannotAcceptResult() {
        KilnProcessingPlan.Result result = KilnProcessingPlan.advance(3, 10, 1, 1, false);

        assertThat(result.progress()).isEqualTo(3);
        assertThat(result.consumedEnergy()).isFalse();
        assertThat(result.lit()).isFalse();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void advance_consumesEnergyAndIncrementsProgress() {
        KilnProcessingPlan.Result result = KilnProcessingPlan.advance(3, 10, 1, 1, true);

        assertThat(result.progress()).isEqualTo(4);
        assertThat(result.consumedEnergy()).isTrue();
        assertThat(result.lit()).isTrue();
        assertThat(result.complete()).isFalse();
    }

    @Test
    void advance_completesWhenNextProgressReachesCookingTime() {
        KilnProcessingPlan.Result result = KilnProcessingPlan.advance(9, 10, 1, 1, true);

        assertThat(result.progress()).isEqualTo(10);
        assertThat(result.consumedEnergy()).isTrue();
        assertThat(result.lit()).isTrue();
        assertThat(result.complete()).isTrue();
    }

    @Test
    void acceptsOutput_acceptsEmptyOutputSlot() {
        assertThat(KilnProcessingPlan.acceptsOutput(ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT))).isTrue();
    }

    @Test
    void acceptsOutput_acceptsMatchingStackWhenResultFits() {
        ItemStack output = new ItemStack(Items.IRON_INGOT, 63);
        ItemStack result = new ItemStack(Items.IRON_INGOT);

        assertThat(KilnProcessingPlan.acceptsOutput(output, result)).isTrue();
    }

    @Test
    void acceptsOutput_rejectsMatchingStackWhenResultWouldOverflow() {
        ItemStack output = new ItemStack(Items.IRON_INGOT, 64);
        ItemStack result = new ItemStack(Items.IRON_INGOT);

        assertThat(KilnProcessingPlan.acceptsOutput(output, result)).isFalse();
    }

    @Test
    void acceptsOutput_rejectsDifferentOutputStack() {
        ItemStack output = new ItemStack(Items.GOLD_INGOT);
        ItemStack result = new ItemStack(Items.IRON_INGOT);

        assertThat(KilnProcessingPlan.acceptsOutput(output, result)).isFalse();
    }
}
