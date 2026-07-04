package com.logistics.automation.alloysmelter;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.ItemResult;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Alloy Smelter recipe — two unordered inputs combined into a primary result plus an optional
 * chance-based byproduct. Loaded by vanilla's {@code RecipeManager} under the
 * {@code logistics:alloy_smelter} type.
 *
 * <p><b>Input order does not matter:</b> a recipe writes {@code input_a}/{@code input_b} once and
 * matches whichever physical slot holds which item (so {@code Sand + Cobblestone} equals
 * {@code Cobblestone + Sand}). The resolver derives per-slot consumption counts from the actual
 * placement via {@link #consumptionForSlots}.
 */
public class AlloySmelterRecipe implements Recipe<DualRecipeInput> {

    public static final float DEFAULT_EXPERIENCE = 0.0f;

    private final Ingredient inputA;
    private final int countA;
    private final Ingredient inputB;
    private final int countB;
    private final ItemResult result;
    private final int energy;
    private final float experience;
    private final Optional<AlloySmelterByproduct> byproduct;

    public AlloySmelterRecipe(
            Ingredient inputA, int countA, Ingredient inputB, int countB,
            ItemResult result, int energy, float experience, Optional<AlloySmelterByproduct> byproduct) {
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (countA < 1 || countB < 1) {
            throw new IllegalArgumentException("input counts must be positive, got " + countA + ", " + countB);
        }
        if (!Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("experience must be finite and non-negative, got " + experience);
        }
        this.inputA = inputA;
        this.countA = countA;
        this.inputB = inputB;
        this.countB = countB;
        this.result = result;
        this.energy = energy;
        this.experience = experience;
        this.byproduct = byproduct;
    }

    /** Builds a recipe from the unordered {@code ingredients} pair (slot order is irrelevant to matching). */
    static AlloySmelterRecipe fromIngredients(
            List<CountedIngredient> ingredients, ItemResult result, int energy, float experience,
            Optional<AlloySmelterByproduct> byproduct) {
        CountedIngredient a = ingredients.get(0);
        CountedIngredient b = ingredients.get(1);
        return new AlloySmelterRecipe(
                a.ingredient(), a.count(), b.ingredient(), b.count(), result, energy, experience, byproduct);
    }

    /** Rebuilds a recipe from the two counted inputs (network path). */
    static AlloySmelterRecipe fromCounted(
            CountedIngredient a, CountedIngredient b, MachineResult result, int energy, float experience,
            Optional<AlloySmelterByproduct> byproduct) {
        return new AlloySmelterRecipe(
                a.ingredient(), a.count(), b.ingredient(), b.count(), result, energy, experience, byproduct);
    }

    /** The two inputs as a counted-ingredient list, for serialization. */
    public List<CountedIngredient> ingredientList() {
        return List.of(new CountedIngredient(inputA, countA), new CountedIngredient(inputB, countB));
    }

    public CountedIngredient countedA() {
        return new CountedIngredient(inputA, countA);
    }

    public CountedIngredient countedB() {
        return new CountedIngredient(inputB, countB);
    }

    public Ingredient inputA() {
        return inputA;
    }

    public int countA() {
        return countA;
    }

    public Ingredient inputB() {
        return inputB;
    }

    public int countB() {
        return countB;
    }

    public ItemResult result() {
        return result;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    public Optional<AlloySmelterByproduct> byproduct() {
        return byproduct;
    }

    /** The primary result as a fresh stack. */
    public ItemStack getResultItem() {
        return result.toStack();
    }

    /** Whether {@code stack} can satisfy either input (used to gate insertion into the input slots). */
    public boolean acceptsAsInput(ItemStack stack) {
        return inputA.test(stack) || inputB.test(stack);
    }

    @Override
    public boolean matches(@NotNull DualRecipeInput input, @NotNull Level level) {
        return matchesOrientation(input.first(), input.second())
                || matchesOrientation(input.second(), input.first());
    }

    private boolean matchesOrientation(ItemStack forA, ItemStack forB) {
        return inputA.test(forA) && forA.getCount() >= countA && inputB.test(forB) && forB.getCount() >= countB;
    }

    /**
     * The per-slot consume counts for the given physical placement, as {@code {slot0, slot1}}. Assumes
     * {@link #matches} already confirmed one orientation holds; prefers A→slot0/B→slot1 and falls back
     * to the swapped orientation.
     */
    public int[] consumptionForSlots(ItemStack slot0, ItemStack slot1) {
        if (matchesOrientation(slot0, slot1)) {
            return new int[] {countA, countB};
        }
        return new int[] {countB, countA};
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull DualRecipeInput input, HolderLookup.@NotNull Provider provider) {
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
    public @NotNull RecipeSerializer<AlloySmelterRecipe> getSerializer() {
        return AlloySmelterRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<AlloySmelterRecipe> getType() {
        return LogisticsAutomation.RECIPE.ALLOY_SMELTER_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }
}
