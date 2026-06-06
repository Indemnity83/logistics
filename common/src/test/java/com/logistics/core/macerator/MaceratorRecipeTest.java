package com.logistics.core.macerator;

import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaceratorRecipeWrapper")
class MaceratorRecipeTest extends MinecraftTestEnvironment {

    private static MaceratorRecipeWrapper rawIronRecipe() {
        return new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_GRINDING_TIME,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE
        );
    }

    // ==================== matches(ItemStack) ====================

    @Test
    @DisplayName("should match when ingredient matches input stack")
    void matchesCorrectItem() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.RAW_IRON))).isTrue();
    }

    @Test
    @DisplayName("should not match when ingredient does not match input stack")
    void doesNotMatchWrongItem() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.COAL))).isFalse();
    }

    @Test
    @DisplayName("should not match empty stack")
    void doesNotMatchEmptyStack() {
        assertThat(rawIronRecipe().matches(ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("should match stack with count greater than 1")
    void matchesStackWithMultipleItems() {
        assertThat(rawIronRecipe().matches(new ItemStack(Items.RAW_IRON, 5))).isTrue();
    }

    // ==================== getters ====================

    @Test
    @DisplayName("should return correct result item count")
    void resultItemCount() {
        ItemStack result = rawIronRecipe().getResultItem();
        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.is(Items.IRON_INGOT)).isTrue();
    }

    @Test
    @DisplayName("should preserve non-default grinding time")
    void grindingTime() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            5,
            MaceratorRecipeWrapper.DEFAULT_EXPERIENCE
        );
        assertThat(recipe.grindingTime()).isEqualTo(5);
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        MaceratorRecipeWrapper recipe = new MaceratorRecipeWrapper(
            Ingredient.of(Items.RAW_IRON),
            new ItemStackTemplate(Items.IRON_INGOT, 2),
            MaceratorRecipeWrapper.DEFAULT_GRINDING_TIME,
            0.7f
        );
        assertThat(recipe.experience()).isEqualTo(0.7f);
    }
}
