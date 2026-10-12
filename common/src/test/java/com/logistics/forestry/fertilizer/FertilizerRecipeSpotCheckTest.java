package com.logistics.forestry.fertilizer;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.test.RecipeJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Design claim (0202 Fertilizer &amp; Soils): "Apatite + 2 Sand → 4" and "Apatite Dust + Saltpeter +
 * Pulped Biomass → 8", both bench recipes.
 */
@DisplayName("Fertilizer recipes (spot check)")
class FertilizerRecipeSpotCheckTest {

    private static final String FERTILIZER = "logistics:forestry/fertilizer";

    private static JsonObject loadRecipe(String file) throws IOException {
        String path = "data/logistics/recipe/forestry/" + file + ".json";
        try (InputStream stream =
                FertilizerRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> ingredients(JsonObject recipe) {
        return recipe.getAsJsonArray("ingredients").asList().stream().map(RecipeJson::ingredient).toList();
    }

    private static void assertResult(JsonObject recipe, int count) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo(FERTILIZER);
        assertThat(result.get("count").getAsInt()).isEqualTo(count);
    }

    @Test
    @DisplayName("an apatite gem and two sand make four, shapeless")
    void fromApatiteAndSand() throws IOException {
        JsonObject recipe = loadRecipe("fertilizer");

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shapeless");
        assertThat(ingredients(recipe))
                .containsExactlyInAnyOrder("#c:gems/apatite", "#minecraft:sand", "#minecraft:sand");
        assertResult(recipe, 4);
    }

    @Test
    @DisplayName("apatite dust, saltpeter and pulped biomass make eight, shapeless")
    void fromApatiteDustSaltpeterAndBiomass() throws IOException {
        JsonObject recipe = loadRecipe("fertilizer_from_saltpeter");

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shapeless");
        assertThat(ingredients(recipe))
                .containsExactlyInAnyOrder("#c:dusts/apatite", "#c:dusts/saltpeter", "logistics:core/pulped_biomass");
        assertResult(recipe, 8);
    }
}
