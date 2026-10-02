package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins every cable tier to one recipe silhouette — a rubber sheath around a core of the tier's own
 * body material, eight at a time — so the ladder cannot drift back into per-tier shapes.
 *
 * <p>The body material is the thing that identifies the tier, so it is asserted per tier rather
 * than left to a reviewer's eye: swapping Lumenite for Echonite here would silently reorder the
 * ladder while every other file still looked right.
 */
@DisplayName("Cable recipes (spot check)")
class CableRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String tier) throws IOException {
        String path = "data/logistics/recipe/power/" + tier + "_cable.json";
        try (InputStream stream = CableRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "basic,minecraft:copper_ingot",
        "conductive,logistics:core/caterium_ingot",
        "resonant,logistics:core/lumenite_ingot",
        "deep,logistics:core/echonite_ingot"
    })
    @DisplayName("every tier is a rubber sheath around its own body material, 8 at a time")
    void cableMatchesTheSharedSilhouette(String tier, String body) throws IOException {
        JsonObject recipe = loadRecipe(tier);

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        List<String> pattern =
                recipe.getAsJsonArray("pattern").asList().stream().map(e -> e.getAsString()).toList();
        assertThat(pattern).containsExactly("RRR", "BBB", "RRR");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(ingredient(key.get("R"))).as("sheath is always rubber").isEqualTo("logistics:core/rubber");
        assertThat(ingredient(key.get("B"))).as("core is the tier's body material").isEqualTo(body);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:power/" + tier + "_cable");
        assertThat(result.get("count").getAsInt()).isEqualTo(8);
    }

    /**
     * An ingredient in its canonical {@code #tag} / {@code item} spelling.
     *
     * <p>Vanilla accepts a bare string for an {@code Ingredient} on 26.x but demands an object on
     * 1.21.1, so the shipped JSON differs by branch while the recipe does not. Normalising here
     * keeps the assertions — and this file — the same across branches.
     */
    private static String ingredient(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        JsonObject object = element.getAsJsonObject();
        return object.has("tag") ? "#" + object.get("tag").getAsString() : object.get("item").getAsString();
    }
}
