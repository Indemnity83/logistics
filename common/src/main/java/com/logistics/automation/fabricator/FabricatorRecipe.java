package com.logistics.automation.fabricator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.AbstractLogisticsRecipe;
import com.logistics.core.lib.recipe.ItemResult;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Assembly-style recipe for the Sequential Fabricator — a list of sized ingredients drawn from the
 * machine's general material pool, an item result, and an RF cost. Loaded by vanilla's
 * {@code RecipeManager} under the {@code logistics:fabricator} type.
 *
 * <p>Ingredient availability is checked against the whole 12-slot pool via {@link #canCraftFrom},
 * not vanilla grid matching, so the recipe carries no slot layout.
 */
public class FabricatorRecipe extends AbstractLogisticsRecipe<FabricatorInput> {

    private final List<SizedIngredient> ingredients;
    private final ItemResult result;
    private final int energy;

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

    /** Reports one greedy allocation from {@link #allocateFrom}: {@code slot} contributed {@code amount}. */
    @FunctionalInterface
    public interface Allocation {
        void take(int slot, int amount);
    }

    /**
     * Greedy per-ingredient / per-slot allocation over {@code pool}: for each ingredient, walk the
     * slots in order taking {@code Math.min(available, needed)} until satisfied, never counting a slot
     * toward two ingredients. Each take is reported to {@code sink}. Returns {@code true} iff every
     * ingredient is fully satisfied (stopping short may already have reported partial takes).
     *
     * <p>Shared by {@link #canCraftFrom} (no-op sink) and the machine's consume step so the
     * availability check and the actual consumption can never diverge.
     */
    public boolean allocateFrom(List<ItemStack> pool, Allocation sink) {
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
                    sink.take(i, take);
                }
            }
            if (needed > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether {@code pool} holds enough of every ingredient. Greedy allocation over a working copy of
     * the pool counts — good for the distinct-ingredient recipes shipped here; a slot is never counted
     * toward two ingredients.
     */
    public boolean canCraftFrom(List<ItemStack> pool) {
        return allocateFrom(pool, (slot, amount) -> {});
    }

    @Override
    public boolean matches(@NotNull FabricatorInput input, @NotNull Level level) {
        return canCraftFrom(input.items());
    }

    @Override
    protected @NotNull ItemStack assembleResult() {
        return result.toStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
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
}
