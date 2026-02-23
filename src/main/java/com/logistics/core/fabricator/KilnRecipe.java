package com.logistics.core.fabricator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Recipe for kiln annealing.
 * Requires a 3x3 grid pattern, molten glass, and heat to produce valves.
 *
 * Note: This doesn't implement Recipe<T> interface since we use Container directly
 * and don't need the full Recipe API integration for this custom machine.
 */
public class KilnRecipe {
    private final Identifier id;
    private final List<Optional<Ingredient>> ingredients; // 9 slots (3x3 grid)
    private final int requiredHeat;
    private final int soakTicks;
    private final int moltenCost;  // millibuckets
    private final int heatCost;
    private final ItemStack result;

    public KilnRecipe(
        Identifier id,
        List<Optional<Ingredient>> ingredients,
        int requiredHeat,
        int soakTicks,
        int moltenCost,
        int heatCost,
        ItemStack result
    ) {
        this.id = id;
        this.ingredients = ingredients;
        this.requiredHeat = requiredHeat;
        this.soakTicks = soakTicks;
        this.moltenCost = moltenCost;
        this.heatCost = heatCost;
        this.result = result;
    }

    public Identifier getId() {
        return id;
    }

    public boolean matches(Container inv, int gridStartSlot) {
        for (int i = 0; i < 9; i++) {
            Optional<Ingredient> ingredient = ingredients.get(i);
            ItemStack slotStack = inv.getItem(i + gridStartSlot);

            // Use vanilla's helper that handles Optional correctly
            if (!Ingredient.testOptionalIngredient(ingredient, slotStack)) {
                return false;
            }
        }
        return true;
    }

    public ItemStack getResultItem() {
        return result;
    }

    // Getters for recipe parameters
    public int getRequiredHeat() {
        return requiredHeat;
    }

    public int getSoakTicks() {
        return soakTicks;
    }

    public int getMoltenCost() {
        return moltenCost;
    }

    public int getHeatCost() {
        return heatCost;
    }

    public List<Optional<Ingredient>> getIngredients() {
        return ingredients;
    }

    /**
     * Consumes ingredients from the grid starting at the specified slot.
     */
    public void consumeIngredients(Container inv, int gridStartSlot) {
        for (int i = 0; i < 9; i++) {
            Optional<Ingredient> ingredient = ingredients.get(i);
            if (ingredient.isPresent()) {
                // Only consume non-empty slots
                ItemStack stack = inv.getItem(gridStartSlot + i);
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                }
            }
        }
    }

}
