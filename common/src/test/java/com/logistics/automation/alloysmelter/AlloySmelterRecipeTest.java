package com.logistics.automation.alloysmelter;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.recipe.MachineResult;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.Test;

/** The Alloy Smelter's two inputs are unordered: a recipe matches regardless of which slot holds which. */
class AlloySmelterRecipeTest extends MinecraftTestEnvironment {

    /** copper x3 + gold x1 -> (stand-in) result; counts differ to prove per-slot consumption tracks placement. */
    private static AlloySmelterRecipe recipe() {
        return new AlloySmelterRecipe(
                Ingredient.of(Items.COPPER_INGOT), 3,
                Ingredient.of(Items.GOLD_INGOT), 1,
                MachineResult.of(Items.IRON_INGOT, 4), 4000, 0f, Optional.empty());
    }

    @Test
    void matchesInEitherInputOrder() {
        AlloySmelterRecipe recipe = recipe();
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 3);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        assertThat(recipe.matches(new DualRecipeInput(copper, gold), null)).isTrue();
        assertThat(recipe.matches(new DualRecipeInput(gold, copper), null)).isTrue();
    }

    @Test
    void consumptionFollowsPhysicalPlacement() {
        AlloySmelterRecipe recipe = recipe();
        ItemStack copper = new ItemStack(Items.COPPER_INGOT, 3);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT, 1);

        // input A (copper x3) in slot 0, input B (gold x1) in slot 1
        assertThat(recipe.consumptionForSlots(copper, gold)).containsExactly(3, 1);
        // swapped: gold in slot 0, copper in slot 1
        assertThat(recipe.consumptionForSlots(gold, copper)).containsExactly(1, 3);
    }

    @Test
    void requiresBothInputsPresentAndSufficient() {
        AlloySmelterRecipe recipe = recipe();

        // Only one input present.
        assertThat(recipe.matches(
                new DualRecipeInput(new ItemStack(Items.COPPER_INGOT, 3), ItemStack.EMPTY), null)).isFalse();
        // Right items, but copper count is short of the required 3.
        assertThat(recipe.matches(
                new DualRecipeInput(new ItemStack(Items.COPPER_INGOT, 2), new ItemStack(Items.GOLD_INGOT, 1)), null))
                .isFalse();
    }
}
