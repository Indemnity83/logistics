package com.logistics.core.fabricator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Custom recipe manager for kiln recipes.
 * Loads recipes from data/<namespace>/recipe/kiln/*.json
 */
public class KilnRecipeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/KilnRecipes");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Identifier, KilnRecipe> RECIPES = new HashMap<>();

    public static void register() {
        LOGGER.info("Registering kiln recipe reload listener");
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.parse("logistics:kiln_recipes");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                LOGGER.info("Kiln recipe reload triggered");
                RECIPES.clear();
                loadRecipes(manager);
            }
        });
    }

    private static void loadRecipes(ResourceManager manager) {
        LOGGER.info("Loading kiln recipes from resources...");
        // Find all recipe files matching data/*/recipes/kiln/*.json
        var resources = manager.listResources("recipes/kiln", path -> path.getPath().endsWith(".json"));
        LOGGER.info("Found {} recipe files", resources.size());
        resources.forEach((resourceLocation, resource) -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);

                    // Extract recipe ID from file path
                    String path = resourceLocation.getPath();
                    String recipeIdPath = path.substring("recipes/kiln/".length(), path.length() - ".json".length());
                    Identifier recipeId = Identifier.parse(resourceLocation.getNamespace() + ":" + recipeIdPath);

                    // Parse manually
                    KilnRecipe recipe = parseRecipe(recipeId, json);
                    RECIPES.put(recipeId, recipe);

                    LOGGER.info("Loaded kiln recipe: {}", recipeId);
                } catch (Exception e) {
                    LOGGER.error("Failed to load kiln recipe {}: {}", resourceLocation, e.getMessage());
                    e.printStackTrace();
                }
            });

        LOGGER.info("Loaded {} kiln recipes", RECIPES.size());
    }

    private static KilnRecipe parseRecipe(Identifier recipeId, JsonObject json) {
        // Parse pattern strings
        JsonArray patternArray = json.getAsJsonArray("pattern");
        List<String> patternList = new ArrayList<>();
        for (JsonElement elem : patternArray) {
            patternList.add(elem.getAsString());
        }

        // Parse key map - each value is a plain string (item ID)
        JsonObject keyObj = json.getAsJsonObject("key");
        Map<Character, Ingredient> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : keyObj.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new IllegalArgumentException("Key must be single character: " + entry.getKey());
            }
            char symbol = entry.getKey().charAt(0);
            String itemId = entry.getValue().getAsString();
            // Create ingredient from item ID
            var itemHolder = BuiltInRegistries.ITEM.get(Identifier.parse(itemId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + itemId));
            Ingredient ingredient = Ingredient.of(itemHolder.value());
            keyMap.put(symbol, ingredient);
        }

        // Use vanilla's ShapedRecipePattern to parse pattern
        // This handles empty slots correctly by converting spaces to Optional.empty()
        ShapedRecipePattern pattern = ShapedRecipePattern.of(keyMap, patternList);

        // Get the parsed ingredients (already as List<Optional<Ingredient>>)
        List<Optional<Ingredient>> ingredients = pattern.ingredients();

        // Parse recipe parameters
        int requiredHeat = json.get("requiredHeat").getAsInt();
        int soakTicks = json.get("soakTicks").getAsInt();
        int moltenCost = json.get("moltenCost").getAsInt();
        int heatCost = json.get("heatCost").getAsInt();

        // Parse result
        ItemStack result = ItemStack.STRICT_CODEC.parse(JsonOps.INSTANCE, json.get("result"))
            .getOrThrow();

        return new KilnRecipe(recipeId, ingredients, requiredHeat, soakTicks, moltenCost, heatCost, result);
    }

    public static Map<Identifier, KilnRecipe> getAllRecipes() {
        return RECIPES;
    }

    public static KilnRecipe getRecipe(Identifier id) {
        return RECIPES.get(id);
    }
}
