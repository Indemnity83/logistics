package com.logistics.core.fabricator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Recipe for kiln annealing.
 * Requires a 3x3 grid pattern, molten glass fluid, and energy to produce valves.
 * The kiln must reach the baseline crafting temperature before any recipe can be processed.
 *
 * Note: This doesn't implement Recipe<T> interface since we use Container directly
 * and don't need the full Recipe API integration for this custom machine.
 */
public class KilnRecipe {
    private final Identifier id;
    private final List<Optional<Ingredient>> ingredients; // 9 slots (3x3 grid)
    private final int processTimeTicks;        // Duration of the crafting process
    private final Identifier fluidId;          // Fluid type (e.g., molten glass)
    private final int fluidAmountMb;           // Fluid amount in millibuckets
    private final int energyPerTick;           // Energy cost per tick (total = energyPerTick * processTimeTicks)
    private final Identifier resultItemId;     // Result item ID (lazy ItemStack creation)
    private final int resultCount;             // Result item count

    public KilnRecipe(
        Identifier id,
        List<Optional<Ingredient>> ingredients,
        int processTimeTicks,
        Identifier fluidId,
        int fluidAmountMb,
        int energyPerTick,
        Identifier resultItemId,
        int resultCount
    ) {
        if (ingredients.size() != 9) {
            throw new IllegalArgumentException("Kiln recipes must have exactly 9 ingredient slots, got " + ingredients.size());
        }
        this.id = id;
        this.ingredients = ingredients;
        this.processTimeTicks = processTimeTicks;
        this.fluidId = fluidId;
        this.fluidAmountMb = fluidAmountMb;
        this.energyPerTick = energyPerTick;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
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
        // Create ItemStack on demand (components are initialized by the time recipes are accessed)
        var itemHolder = BuiltInRegistries.ITEM.get(resultItemId)
            .orElseThrow(() -> new IllegalStateException("Result item not found: " + resultItemId));
        return new ItemStack(itemHolder.value(), resultCount);
    }

    // Getters for recipe parameters
    public int getProcessTimeTicks() {
        return processTimeTicks;
    }

    public Identifier getFluidId() {
        return fluidId;
    }

    public int getFluidAmountMb() {
        return fluidAmountMb;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public List<Optional<Ingredient>> getIngredients() {
        return Collections.unmodifiableList(ingredients);
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
