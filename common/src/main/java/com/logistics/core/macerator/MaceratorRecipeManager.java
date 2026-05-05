package com.logistics.core.macerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.LogisticsMod;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom recipe manager for macerator recipes.
 * Loads recipes from data/<namespace>/recipe/macerator/*.json
 *
 * <p>Recipe JSON format:
 * <pre>{@code
 * {
 *   "ingredient": "minecraft:raw_iron",
 *   "result": { "id": "logistics:core/iron_powder", "count": 2 },
 *   "grindingtime": 200,
 *   "experience": 0.7
 * }
 * }</pre>
 */
public class MaceratorRecipeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Logistics/MaceratorRecipes");
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<ResourceId, MaceratorRecipe> RECIPES = new HashMap<>();

    public static void register() {
        LOGGER.info("Registering macerator recipe reload listener");
        ResourceLocation listenerId = LogisticsMod.modId("macerator_recipes").toIdentifier();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
            new IdentifiableResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return listenerId;
                }

                @Override
                public CompletableFuture<Void> reload(
                    PreparableReloadListener.PreparationBarrier barrier,
                    ResourceManager resourceManager,
                    ProfilerFiller preparationsProfiler,
                    ProfilerFiller reloadProfiler,
                    Executor backgroundExecutor,
                    Executor gameExecutor
                ) {
                    return CompletableFuture.supplyAsync(() -> {
                        LOGGER.info("Macerator recipe reload triggered");
                        Map<ResourceId, MaceratorRecipe> loaded = new HashMap<>();
                        loadRecipesInto(resourceManager, loaded);
                        return loaded;
                    }, backgroundExecutor)
                    .thenCompose(barrier::wait)
                    .thenAcceptAsync(loaded -> {
                        RECIPES.clear();
                        RECIPES.putAll(loaded);
                        LOGGER.info("Applied {} macerator recipes", RECIPES.size());
                    }, gameExecutor);
                }
            }
        );
    }

    private static void loadRecipesInto(ResourceManager manager, Map<ResourceId, MaceratorRecipe> target) {
        LOGGER.info("Loading macerator recipes from resources...");
        var resources = manager.listResources("recipe/macerator", path -> path.getPath().endsWith(".json"));
        LOGGER.info("Found {} macerator recipe files", resources.size());
        resources.forEach((location, resource) -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                String path = location.getPath();
                String recipeIdPath = path.substring("recipe/macerator/".length(), path.length() - ".json".length());
                ResourceId recipeId = ResourceId.in(location.getNamespace(), recipeIdPath);
                MaceratorRecipe recipe = parseRecipe(recipeId, json);
                target.put(recipeId, recipe);
            } catch (Exception e) {
                LOGGER.error("Failed to load macerator recipe {}", location, e);
            }
        });
        LOGGER.info("Loaded {} macerator recipes", target.size());
    }

    private static MaceratorRecipe parseRecipe(ResourceId recipeId, JsonObject json) {
        JsonElement ingredientElement = json.get("ingredient");

        JsonObject resultObj = json.getAsJsonObject("result");
        ResourceId resultItemId = ResourceId.parse(resultObj.get("id").getAsString());
        int resultCount = resultObj.get("count").getAsInt();

        if (BuiltInRegistries.ITEM.get(resultItemId.toIdentifier()) == null) {
            throw new IllegalArgumentException("Unknown result item: " + resultItemId);
        }

        if (ingredientElement == null || ingredientElement.isJsonNull()) {
            throw new IllegalArgumentException("Missing ingredient in recipe: " + recipeId);
        }

        int grindingTime = json.has("grindingtime")
            ? json.get("grindingtime").getAsInt()
            : MaceratorRecipe.DEFAULT_GRINDING_TIME;
        float experience = json.has("experience")
            ? json.get("experience").getAsFloat()
            : MaceratorRecipe.DEFAULT_EXPERIENCE;

        if (ingredientElement.isJsonObject()) {
            JsonObject ingredientObj = ingredientElement.getAsJsonObject();
            if (ingredientObj.has("tag")) {
                String tagId = ingredientObj.get("tag").getAsString();
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
                return new MaceratorRecipe(recipeId, tagKey, resultItemId, resultCount, grindingTime, experience);
            } else if (ingredientObj.has("item")) {
                String itemId = ingredientObj.get("item").getAsString();
                var ingredientItem = BuiltInRegistries.ITEM.get(ResourceId.parse(itemId).toIdentifier());
                if (ingredientItem == null) {
                    throw new IllegalArgumentException("Unknown ingredient item: " + itemId);
                }
                Ingredient ingredient = Ingredient.of(ingredientItem);
                return new MaceratorRecipe(recipeId, ingredient, resultItemId, resultCount, grindingTime, experience);
            } else {
                throw new IllegalArgumentException("Ingredient object must have 'item' or 'tag' field in recipe: " + recipeId);
            }
        } else {
            String ingredientId = ingredientElement.getAsString();
            if (ingredientId.startsWith("#")) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(ingredientId.substring(1)));
                return new MaceratorRecipe(recipeId, tagKey, resultItemId, resultCount, grindingTime, experience);
            }
            var ingredientItem = BuiltInRegistries.ITEM.get(ResourceId.parse(ingredientId).toIdentifier());
            if (ingredientItem == null) {
                throw new IllegalArgumentException("Unknown ingredient item: " + ingredientId);
            }
            Ingredient ingredient = Ingredient.of(ingredientItem);
            return new MaceratorRecipe(recipeId, ingredient, resultItemId, resultCount, grindingTime, experience);
        }
    }

    public static Map<ResourceId, MaceratorRecipe> getAllRecipes() {
        return Collections.unmodifiableMap(RECIPES);
    }

    public static MaceratorRecipe getRecipe(ResourceId id) {
        return RECIPES.get(id);
    }
}
