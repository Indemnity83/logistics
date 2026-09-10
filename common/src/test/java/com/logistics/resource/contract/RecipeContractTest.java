package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.logistics.LogisticsCore;
import com.logistics.test.MinecraftTestEnvironment;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every {@code logistics:} id a shipped recipe names is something we ship.
 *
 * <p>The live counterpart, {@code RecipeLoadingGameTestBody}, proves each file became a recipe — which
 * a misspelled id does not prevent. Vanilla resolves an unknown item id to {@code minecraft:air}
 * rather than failing, so such a recipe loads, appears in JEI, and quietly wants or produces nothing.
 * Checking the ids themselves is therefore a job for a static pass, the same split loot tables use.
 */
@DisplayName("Recipe contract")
class RecipeContractTest extends MinecraftTestEnvironment {

    private static final String RECIPE_DIR = ResourceFiles.NAMESPACE + "/recipe";

    /**
     * The recipe type, checked by loading rather than here: an unknown type fails to deserialize, so
     * the file produces no recipe at all and {@code RecipeLoadingGameTestBody} reports it.
     */
    private static final String TYPE = "type";

    /**
     * Everything a recipe may legitimately name, by id. Fluids are read from their declarations
     * rather than from shipped files because they are registered per loader, so nothing common
     * registers them and no single asset stands for one.
     */
    private static Set<String> shippedIds() {
        Set<String> ids = new TreeSet<>(ResourceFiles.itemDefinitionIds());
        ids.addAll(ResourceFiles.blockstateIds());
        for (LogisticsCore.FluidDef fluid : LogisticsCore.CUSTOM_FLUIDS) {
            ids.add(LogisticsCore.resource(fluid.name()).toString());
            ids.add(LogisticsCore.resource("flowing_" + fluid.name()).toString());
        }
        return ids;
    }

    @Test
    @DisplayName("every logistics id a recipe names is shipped")
    void everyRecipeIdIsShipped() {
        Set<String> shipped = shippedIds();
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.dataJsonFiles(RECIPE_DIR)) {
            JsonObject recipe = ResourceFiles.parse(file);
            // Thirteen recipe types with thirteen schemas: collect every id-shaped string at any
            // depth rather than encoding where each type keeps its ingredients and results, so a
            // new recipe type is covered without a code change.
            for (String id : ourIdsIn(recipe)) {
                if (!shipped.contains(id)) {
                    failures.add(ResourceFiles.describe(file) + " -> names unknown '" + id + "'");
                }
            }
        }

        assertThat(failures)
            .as("recipes naming things we do not ship; each resolves to minecraft:air at load, so "
                + "the recipe still loads and silently wants or produces nothing")
            .isEmpty();
    }

    @Test
    @DisplayName("every recipe declares a type")
    void everyRecipeDeclaresAType() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.dataJsonFiles(RECIPE_DIR)) {
            JsonObject recipe = ResourceFiles.parse(file);
            if (!recipe.has(TYPE) || !recipe.get(TYPE).isJsonPrimitive() || recipe.get(TYPE).getAsString().isBlank()) {
                failures.add(ResourceFiles.describe(file) + " -> no recipe type");
            }
        }

        assertThat(failures).as("recipes with no type").isEmpty();
    }

    /** Sanity guard, not a coverage claim: catches walking an empty or wrong directory. */
    @Test
    @DisplayName("the shipped recipe set has not collapsed")
    void recipeSetHasNotCollapsed() {
        assertThat(ResourceFiles.dataJsonFiles(RECIPE_DIR))
            .as("shipped recipes; lower this floor deliberately if recipes were removed")
            .hasSizeGreaterThanOrEqualTo(600);
    }

    /** Every {@code logistics:}-namespaced string in the recipe, except the recipe type itself. */
    private static Set<String> ourIdsIn(JsonObject recipe) {
        Set<String> ids = new TreeSet<>();
        for (Map.Entry<String, JsonElement> entry : recipe.entrySet()) {
            if (!TYPE.equals(entry.getKey())) {
                collect(entry.getValue(), ids);
            }
        }
        return ids;
    }

    private static void collect(JsonElement element, Set<String> ids) {
        if (element.isJsonObject()) {
            element.getAsJsonObject().asMap().values().forEach(value -> collect(value, ids));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(value -> collect(value, ids));
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith(ResourceFiles.NAMESPACE + ":")) {
                ids.add(value);
            }
        }
    }
}
