package com.logistics.power.cable;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(key.get("R").getAsString()).as("sheath is always rubber").isEqualTo("logistics:core/rubber");
        assertThat(key.get("B").getAsString()).as("core is the tier's body material").isEqualTo(body);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:power/" + tier + "_cable");
        assertThat(result.get("count").getAsInt()).isEqualTo(8);
    }
}
