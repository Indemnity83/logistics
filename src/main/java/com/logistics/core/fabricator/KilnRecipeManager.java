package com.logistics.core.fabricator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.LogisticsMod;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom recipe manager for kiln recipes.
 * Loads recipes from data/<namespace>/recipes/kiln/*.json
 */
public class KilnRecipeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/KilnRecipes");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceLocation, KilnRecipe> RECIPES = new HashMap<>();

    public static void register() {
        LOGGER.info("Registering kiln recipe reload listener");
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiableResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return LogisticsMod.getIdentifier("kiln_recipes");
                }

                @Override
                public CompletableFuture<Void> reload(
                    PreparationBarrier barrier,
                    ResourceManager resourceManager,
                    ProfilerFiller preparationProfiler,
                    ProfilerFiller reloadProfiler,
                    Executor backgroundExecutor,
                    Executor gameExecutor
                ) {
                    // Preparation phase - can run async on background thread
                    return CompletableFuture.supplyAsync(() -> {
                        LOGGER.info("Kiln recipe reload triggered");
                        preparationProfiler.startTick();
                        Map<ResourceLocation, KilnRecipe> loadedRecipes = new HashMap<>();
                        loadRecipesInto(resourceManager, loadedRecipes);
                        preparationProfiler.endTick();
                        return loadedRecipes;
                    }, backgroundExecutor)
                    // Wait for all reload listeners to finish preparation
                    .thenCompose(barrier::wait)
                    // Apply phase - runs on game thread, must be synchronous
                    .thenAcceptAsync(loadedRecipes -> {
                        reloadProfiler.startTick();
                        RECIPES.clear();
                        RECIPES.putAll(loadedRecipes);
                        LOGGER.info("Applied {} kiln recipes", RECIPES.size());
                        reloadProfiler.endTick();
                    }, gameExecutor);
                }
            }
        );
    }

    private static void loadRecipesInto(ResourceManager manager, Map<ResourceLocation, KilnRecipe> targetMap) {
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
                    ResourceLocation recipeId = LogisticsMod.createIdentifier(resourceLocation.getNamespace(), recipeIdPath);

                    // Parse manually
                    KilnRecipe recipe = parseRecipe(recipeId, json);
                    targetMap.put(recipeId, recipe);
                } catch (Exception e) {
                    LOGGER.error("Failed to load kiln recipe {}", resourceLocation, e);
                }
            });

        LOGGER.info("Loaded {} kiln recipes", targetMap.size());
    }

    private static KilnRecipe parseRecipe(ResourceLocation recipeId, JsonObject json) {
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
            var item = BuiltInRegistries.ITEM.get(LogisticsMod.parseIdentifier(itemId));
            if (item == null) {
                throw new IllegalArgumentException("Unknown item: " + itemId);
            }
            Ingredient ingredient = Ingredient.of(item);
            keyMap.put(symbol, ingredient);
        }

        // Use vanilla's ShapedRecipePattern to parse pattern
        // This handles empty slots correctly by converting spaces to Optional.empty()
        ShapedRecipePattern pattern = ShapedRecipePattern.of(keyMap, patternList);

        // Get the parsed ingredients - convert NonNullList to List<Optional<Ingredient>>
        // Empty slots will have Ingredient.EMPTY, which we need to convert to Optional.empty()
        List<Optional<Ingredient>> ingredients = new ArrayList<>();
        for (Ingredient ingredient : pattern.ingredients()) {
            if (ingredient.isEmpty()) {
                ingredients.add(Optional.empty());
            } else {
                ingredients.add(Optional.of(ingredient));
            }
        }

        // Parse recipe parameters
        int processTimeTicks = json.get("processTimeTicks").getAsInt();

        // Parse fluid requirement
        JsonObject fluidObj = json.getAsJsonObject("fluid");
        String fluidIdStr = fluidObj.get("id").getAsString();
        ResourceLocation fluidId = LogisticsMod.parseIdentifier(fluidIdStr);
        int fluidAmountMb = fluidObj.get("amountMb").getAsInt();

        int energyPerTick = json.get("energyPerTick").getAsInt();

        // Parse result ID and count (defer ItemStack creation to avoid component initialization timing issues)
        JsonObject resultObj = json.getAsJsonObject("result");
        String itemIdStr = resultObj.get("id").getAsString();
        int resultCount = resultObj.get("count").getAsInt();
        ResourceLocation resultItemId = LogisticsMod.parseIdentifier(itemIdStr);

        // Validate item exists in registry
        if (BuiltInRegistries.ITEM.get(resultItemId) == null) {
            throw new IllegalArgumentException("Unknown result item: " + itemIdStr);
        }

        return new KilnRecipe(recipeId, ingredients, processTimeTicks, fluidId, fluidAmountMb, energyPerTick, resultItemId, resultCount);
    }

    public static Map<ResourceLocation, KilnRecipe> getAllRecipes() {
        return RECIPES;
    }

    public static KilnRecipe getRecipe(ResourceLocation id) {
        return RECIPES.get(id);
    }
}
