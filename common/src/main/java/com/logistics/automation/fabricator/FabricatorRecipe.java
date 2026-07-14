package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.ItemResult;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assembly-style recipe for the Sequential Fabricator — a list of sized ingredients drawn from the
 * machine's general material pool, an item result, and an RF cost. Loaded by vanilla's
 * {@code RecipeManager} under the {@code logistics:fabricator} type.
 *
 * <p>Ingredient availability is checked against the whole 12-slot pool via {@link #canCraftFrom},
 * not vanilla grid matching, so the recipe carries no slot layout.
 */
public class FabricatorRecipe implements Recipe<FabricatorInput> {

    private final List<SizedIngredient> ingredients;
    private final ItemResult result;
    private final int energy;
    @Nullable private PlacementInfo placementInfo;

    public FabricatorRecipe(List<SizedIngredient> ingredients, ItemResult result, int energy) {
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("fabricator recipe requires at least one ingredient");
        }
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
        this.energy = energy;
    }

    public List<SizedIngredient> ingredients() {
        return ingredients;
    }

    public ItemResult result() {
        return result;
    }

    /** Total RF this craft consumes; the machine drains it at a fixed RF/t. */
    public int energy() {
        return energy;
    }

    /** The result as a fresh stack. */
    public ItemStack getResultItem() {
        return result.toStack();
    }

    /**
     * Whether {@code pool} holds enough of every ingredient. Greedy allocation over a working copy of
     * the pool counts — good for the distinct-ingredient recipes shipped here; a slot is never counted
     * toward two ingredients.
     */
    public boolean canCraftFrom(List<ItemStack> pool) {
        int[] remaining = new int[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            remaining[i] = pool.get(i).getCount();
        }
        for (SizedIngredient ingredient : ingredients) {
            int needed = ingredient.count();
            for (int i = 0; i < pool.size() && needed > 0; i++) {
                if (remaining[i] > 0 && ingredient.test(pool.get(i))) {
                    int take = Math.min(remaining[i], needed);
                    remaining[i] -= take;
                    needed -= take;
                }
            }
            if (needed > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matches(@NotNull FabricatorInput input, @NotNull Level level) {
        return canCraftFrom(input.items());
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull FabricatorInput input) {
        return result.toStack();
    }

    @Override
    public @NotNull RecipeSerializer<FabricatorRecipe> getSerializer() {
        return FabricatorRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<FabricatorRecipe> getType() {
        return LogisticsAutomation.RECIPE.FABRICATOR_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(ingredients.stream().map(SizedIngredient::ingredient).toList());
        }
        return placementInfo;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return LogisticsAutomation.RECIPE.FABRICATOR_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        // Custom GUI drives its own output preview; no recipe-book display.
        return List.of();
    }
}
