package com.logistics.core.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

/**
 * Sided-insertion gate for a machine's input slots: admits only stacks some loaded recipe of the
 * machine's own recipe class could use. A machine without one accepts anything automation offers
 * into an input slot that no face exposes for extraction, so a wrongly-routed item jams it until a
 * player clears it by hand.
 *
 * <p>The test is ingredient-only and ignores stack counts: a single delivery rarely meets a
 * recipe's full ingredient count on its own, so a count-aware gate would refuse every partial
 * delivery. The recipe processor re-checks counts on the server tick.
 *
 * <p>Recipes are cached against the server's current {@code RecipeManager} because the probe runs
 * on every hopper and pipe insertion attempt; a data-pack reload swaps the manager and rebuilds the
 * cache. Filtering the full recipe set by class keeps this loader-agnostic — {@code
 * recipeMap().byType()} is a NeoForge patch.
 */
public final class RecipeInputFilter<T> implements Predicate<ItemStack> {

    private final MachineContext host;
    private final Class<T> recipeClass;
    private final BiPredicate<T, ItemStack> acceptsAsInput;

    @Nullable private RecipeManager cachedManager;
    private List<T> cachedRecipes = List.of();

    private RecipeInputFilter(
            MachineContext host, Class<T> recipeClass, BiPredicate<T, ItemStack> acceptsAsInput) {
        this.host = host;
        this.recipeClass = recipeClass;
        this.acceptsAsInput = acceptsAsInput;
    }

    /**
     * A filter over {@code recipeClass}'s loaded recipes, asking {@code acceptsAsInput} whether each
     * could take the offered stack as an input.
     */
    public static <T> RecipeInputFilter<T> of(
            MachineContext host, Class<T> recipeClass, BiPredicate<T, ItemStack> acceptsAsInput) {
        return new RecipeInputFilter<>(host, recipeClass, acceptsAsInput);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        RecipeManager recipeManager = host.recipeManager();
        if (recipeManager == null) {
            return true; // permissive during load / on the client; validated again on the server tick
        }
        for (T recipe : recipes(recipeManager)) {
            if (acceptsAsInput.test(recipe, stack)) {
                return true;
            }
        }
        return false;
    }

    private List<T> recipes(RecipeManager recipeManager) {
        if (recipeManager != cachedManager) {
            List<T> recipes = new ArrayList<>();
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (recipeClass.isInstance(holder.value())) {
                    recipes.add(recipeClass.cast(holder.value()));
                }
            }
            cachedManager = recipeManager;
            cachedRecipes = List.copyOf(recipes);
        }
        return cachedRecipes;
    }
}
