package com.logistics.core.macerator;

import com.logistics.core.lib.resource.ResourceId;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaceratorRecipe")
class MaceratorRecipeTest extends MinecraftTestEnvironment {

    private static MaceratorRecipe rawIronRecipe() {
        return new MaceratorRecipe(
            ResourceId.in("test", "iron_dust"),
            Ingredient.of(Items.RAW_IRON),
            ResourceId.in("minecraft", "iron_ingot"), // using iron_ingot as a valid item for result
            2,
            MaceratorRecipe.DEFAULT_GRINDING_TIME,
            MaceratorRecipe.DEFAULT_EXPERIENCE
        );
    }

    // ==================== matches() ====================

    @Test
    @DisplayName("should match when ingredient matches input stack")
    void matchesCorrectItem() {
        MaceratorRecipe recipe = rawIronRecipe();
        assertThat(recipe.matches(new ItemStack(Items.RAW_IRON))).isTrue();
    }

    @Test
    @DisplayName("should not match when ingredient does not match input stack")
    void doesNotMatchWrongItem() {
        MaceratorRecipe recipe = rawIronRecipe();
        assertThat(recipe.matches(new ItemStack(Items.COAL))).isFalse();
    }

    @Test
    @DisplayName("should not match empty stack")
    void doesNotMatchEmptyStack() {
        MaceratorRecipe recipe = rawIronRecipe();
        assertThat(recipe.matches(ItemStack.EMPTY)).isFalse();
    }

    @Test
    @DisplayName("should match stack with count greater than 1")
    void matchesStackWithMultipleItems() {
        MaceratorRecipe recipe = rawIronRecipe();
        ItemStack stack = new ItemStack(Items.RAW_IRON, 5);
        assertThat(recipe.matches(stack)).isTrue();
    }

    // ==================== getters ====================

    @Test
    @DisplayName("should return correct result item count")
    void resultItemCount() {
        MaceratorRecipe recipe = rawIronRecipe();
        ItemStack result = recipe.getResultItem();
        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.is(Items.IRON_INGOT)).isTrue();
    }

    @Test
    @DisplayName("should preserve non-default grinding time")
    void grindingTime() {
        MaceratorRecipe recipe = new MaceratorRecipe(
            ResourceId.in("test", "iron_dust"),
            Ingredient.of(Items.RAW_IRON),
            ResourceId.in("minecraft", "iron_ingot"),
            2,
            5,
            MaceratorRecipe.DEFAULT_EXPERIENCE
        );
        assertThat(recipe.getGrindingTime()).isEqualTo(5);
    }

    @Test
    @DisplayName("should preserve non-default experience")
    void experience() {
        MaceratorRecipe recipe = new MaceratorRecipe(
            ResourceId.in("test", "iron_dust"),
            Ingredient.of(Items.RAW_IRON),
            ResourceId.in("minecraft", "iron_ingot"),
            2,
            MaceratorRecipe.DEFAULT_GRINDING_TIME,
            0.7f
        );
        assertThat(recipe.getExperience()).isEqualTo(0.7f);
    }

    @Test
    @DisplayName("should return correct recipe id")
    void recipeId() {
        MaceratorRecipe recipe = rawIronRecipe();
        assertThat(recipe.getId().toString()).isEqualTo("test:iron_dust");
    }
}
