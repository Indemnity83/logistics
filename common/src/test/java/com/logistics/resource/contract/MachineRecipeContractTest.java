package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Machines are gated behind the machine-component tier: one shipped crafting recipe each, built
 * around a machine core and a redstone reception coil. A second, cheaper recipe anywhere in the
 * data tree silently replaces that gate, since players craft whichever is cheaper.
 */
@DisplayName("Machine recipe contract")
class MachineRecipeContractTest {

    private static final String RECIPE_DIR = ResourceFiles.NAMESPACE + "/recipe";
    private static final String MACHINE_PREFIX = ResourceFiles.NAMESPACE + ":automation/";
    private static final String MACHINE_CORE = ResourceFiles.NAMESPACE + ":core/machine_core";
    private static final String RECEPTION_COIL = ResourceFiles.NAMESPACE + ":core/redstone_reception_coil";

    @Test
    @DisplayName("every machine has exactly one crafting recipe")
    void everyMachineHasExactlyOneCraftingRecipe() {
        List<String> failures = new ArrayList<>();

        machineRecipes().forEach((machine, files) -> {
            if (files.size() > 1) {
                failures.add(machine + " -> " + files.stream().map(ResourceFiles::describe).sorted().toList());
            }
        });

        assertThat(failures)
            .as("machines with more than one crafting recipe; the cheapest wins and the rest are dead")
            .isEmpty();
    }

    @Test
    @DisplayName("every machine recipe is built around the machine components")
    void everyMachineRecipeUsesTheMachineComponents() {
        List<String> failures = new ArrayList<>();

        machineRecipes().forEach((machine, files) -> {
            for (Path file : files) {
                Set<String> ingredients = ingredientsOf(ResourceFiles.parse(file));
                if (!ingredients.contains(MACHINE_CORE) || !ingredients.contains(RECEPTION_COIL)) {
                    failures.add(ResourceFiles.describe(file) + " -> " + new TreeSet<>(ingredients));
                }
            }
        });

        assertThat(failures)
            .as("machine recipes that skip the machine core or the reception coil")
            .isEmpty();
    }

    /** Crafting recipes producing a machine, keyed by the machine they produce. */
    private static Map<String, List<Path>> machineRecipes() {
        Map<String, List<Path>> byMachine = new LinkedHashMap<>();

        for (Path file : ResourceFiles.dataJsonFiles(RECIPE_DIR)) {
            JsonObject recipe = ResourceFiles.parse(file);
            if (!recipe.get("type").getAsString().startsWith("minecraft:crafting")) {
                continue;
            }
            String result = resultOf(recipe);
            if (result != null && result.startsWith(MACHINE_PREFIX)) {
                byMachine.computeIfAbsent(result, key -> new ArrayList<>()).add(file);
            }
        }

        assertThat(byMachine).as("shipped machine crafting recipes; an empty map would pass every check").isNotEmpty();
        return byMachine;
    }

    private static String resultOf(JsonObject recipe) {
        JsonElement result = recipe.get("result");
        if (result == null) {
            return null;
        }
        return result.isJsonObject() ? result.getAsJsonObject().get("id").getAsString() : result.getAsString();
    }

    /** Ingredient ids of a shaped or shapeless recipe, whichever spelling the branch's format uses. */
    private static Set<String> ingredientsOf(JsonObject recipe) {
        Set<String> ids = new TreeSet<>();
        if (recipe.has("key")) {
            recipe.getAsJsonObject("key").asMap().values().forEach(value -> collectIds(value, ids));
        }
        if (recipe.has("ingredients")) {
            recipe.getAsJsonArray("ingredients").forEach(value -> collectIds(value, ids));
        }
        return ids;
    }

    /** An ingredient is a bare id, an array of them, or an object spelling one as {@code item}/{@code tag}. */
    private static void collectIds(JsonElement ingredient, Set<String> ids) {
        if (ingredient.isJsonPrimitive()) {
            ids.add(ingredient.getAsString());
        } else if (ingredient.isJsonArray()) {
            ingredient.getAsJsonArray().forEach(child -> collectIds(child, ids));
        } else if (ingredient.isJsonObject()) {
            JsonObject object = ingredient.getAsJsonObject();
            for (String field : List.of("item", "tag")) {
                if (object.has(field)) {
                    collectIds(object.get(field), ids);
                }
            }
        }
    }
}
