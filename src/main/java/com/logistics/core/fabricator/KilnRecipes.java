package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Recipe system for the Kiln.
 * Uses pattern matching in a 3x3 grid to determine electron tube output.
 *
 * <p>Pattern (all tubes):
 * <pre>
 * [M] [M] [M]
 * [M] [C] [M]  (M = material, C = center item: redstone or ender eye)
 * [M] [C] [M]
 * </pre>
 *
 * <p>Requirements:
 * - 7 material items in specific positions
 * - 2 center items (redstone or ender eye for ender tubes)
 * - 1000mb molten glass in tank
 * - Minimum temperature based on material tier
 * → Produces 4 electron tubes
 */
public final class KilnRecipes {

    /** Molten glass required per craft (in millibuckets). */
    public static final long MOLTEN_GLASS_PER_CRAFT = 1000;

    /** Output count for all recipes. */
    public static final int OUTPUT_COUNT = 4;

    /**
     * Recipe definition for pattern-based electron tube crafting.
     *
     * @param outputTube electron tube item produced
     * @param material material item for grid (7 pieces)
     * @param centerItem center item (redstone or ender eye, 2 pieces)
     * @param requiredTemp minimum temperature in °C
     */
    public record RecipePattern(
        Item outputTube,
        Item material,
        Item centerItem,
        int requiredTemp
    ) {}

    // Material → Recipe mapping
    private static final Map<Item, RecipePattern> MATERIAL_RECIPES = new HashMap<>();

    static {
        // Low-tier tubes (2000-4000°C)
        register(Items.COPPER_INGOT, LogisticsCore.ITEM.ELECTRON_TUBE_COPPER, Items.REDSTONE, 2000);
        register(LogisticsCore.ITEM.TIN_INGOT, LogisticsCore.ITEM.ELECTRON_TUBE_TIN, Items.REDSTONE, 2000);
        register(LogisticsCore.ITEM.BRONZE_INGOT, LogisticsCore.ITEM.ELECTRON_TUBE_BRONZE, Items.REDSTONE, 2500);
        register(Items.IRON_INGOT, LogisticsCore.ITEM.ELECTRON_TUBE_IRON, Items.REDSTONE, 3000);

        // Mid-tier tubes (4000-7000°C)
        register(Items.GOLD_INGOT, LogisticsCore.ITEM.ELECTRON_TUBE_GOLD, Items.REDSTONE, 4000);
        register(Items.OBSIDIAN, LogisticsCore.ITEM.ELECTRON_TUBE_OBSIDIAN, Items.REDSTONE, 5000);
        register(Items.BLAZE_POWDER, LogisticsCore.ITEM.ELECTRON_TUBE_BLAZING, Items.REDSTONE, 6000);

        // High-tier tubes (7000-9500°C)
        register(Items.EMERALD, LogisticsCore.ITEM.ELECTRON_TUBE_EMERALD, Items.REDSTONE, 7000);
        register(Items.DIAMOND, LogisticsCore.ITEM.ELECTRON_TUBE_DIAMOND, Items.REDSTONE, 8000);
        register(LogisticsCore.ITEM.APATITE, LogisticsCore.ITEM.ELECTRON_TUBE_APATINE, Items.REDSTONE, 8500);
        register(Items.LAPIS_LAZULI, LogisticsCore.ITEM.ELECTRON_TUBE_LAPIS, Items.REDSTONE, 9000);
        register(Items.ENDER_PEARL, LogisticsCore.ITEM.ELECTRON_TUBE_ENDER, Items.ENDER_EYE, 9500);
    }

    private static void register(Item material, Item outputTube, Item centerItem, int requiredTemp) {
        MATERIAL_RECIPES.put(material, new RecipePattern(outputTube, material, centerItem, requiredTemp));
    }

    /**
     * Grid slot positions for the 3x3 crafting grid.
     * Slots 0-8 in inventory correspond to this pattern:
     * <pre>
     * [0] [1] [2]
     * [3] [4] [5]
     * [6] [7] [8]
     * </pre>
     */
    private static final int[] MATERIAL_SLOTS = {0, 1, 2, 3, 5, 6, 8}; // 7 material positions
    private static final int[] CENTER_SLOTS = {4, 7}; // 2 center positions

    /**
     * Finds a matching recipe for the current grid contents.
     * Returns null if no valid recipe is found.
     *
     * @param inv the inventory container
     * @param gridStart starting slot index for the 3x3 grid
     * @return matching recipe or null
     */
    @Nullable
    public static RecipePattern findRecipeForGrid(Container inv, int gridStart) {
        // Count materials in grid to determine dominant type
        Map<Item, Integer> materialCounts = new HashMap<>();
        for (int offset : MATERIAL_SLOTS) {
            ItemStack stack = inv.getItem(gridStart + offset);
            if (!stack.isEmpty()) {
                materialCounts.merge(stack.getItem(), 1, Integer::sum);
            }
        }

        // Find material with exactly 7 pieces
        Item dominantMaterial = null;
        for (Map.Entry<Item, Integer> entry : materialCounts.entrySet()) {
            if (entry.getValue() == 7) {
                dominantMaterial = entry.getKey();
                break;
            }
        }

        if (dominantMaterial == null) {
            return null; // No dominant material or wrong count
        }

        // Look up recipe for this material
        RecipePattern recipe = MATERIAL_RECIPES.get(dominantMaterial);
        if (recipe == null) {
            return null; // Unknown material
        }

        // Validate pattern matches recipe
        if (!matchesPattern(recipe, inv, gridStart)) {
            return null;
        }

        return recipe;
    }

    /**
     * Validates that the grid matches the expected pattern for a recipe.
     *
     * @param pattern the recipe pattern to validate
     * @param inv the inventory container
     * @param gridStart starting slot index for the 3x3 grid
     * @return true if pattern matches
     */
    private static boolean matchesPattern(RecipePattern pattern, Container inv, int gridStart) {
        // Check material slots
        for (int offset : MATERIAL_SLOTS) {
            ItemStack stack = inv.getItem(gridStart + offset);
            if (stack.isEmpty() || !stack.is(pattern.material)) {
                return false;
            }
        }

        // Check center slots
        for (int offset : CENTER_SLOTS) {
            ItemStack stack = inv.getItem(gridStart + offset);
            if (stack.isEmpty() || !stack.is(pattern.centerItem)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Consumes ingredients from the grid after successful crafting.
     *
     * @param inv the inventory container
     * @param gridStart starting slot index for the 3x3 grid
     */
    public static void consumeIngredients(Container inv, int gridStart) {
        // Shrink material stacks
        for (int offset : MATERIAL_SLOTS) {
            ItemStack stack = inv.getItem(gridStart + offset);
            if (!stack.isEmpty()) {
                stack.shrink(1);
            }
        }

        // Shrink center item stacks
        for (int offset : CENTER_SLOTS) {
            ItemStack stack = inv.getItem(gridStart + offset);
            if (!stack.isEmpty()) {
                stack.shrink(1);
            }
        }
    }

    private KilnRecipes() {
        // Utility class
    }
}
