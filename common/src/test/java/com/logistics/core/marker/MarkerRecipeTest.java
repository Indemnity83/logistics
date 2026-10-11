package com.logistics.core.marker;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.test.RecipeJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the Marker's actual shipped crafting recipe against what the wiki documents, reading the
 * bundled JSON directly rather than a hand-built stand-in.
 */
@DisplayName("Marker crafting recipe")
class MarkerRecipeTest {

    private static final String RECIPE_PATH = "data/logistics/recipe/core/marker.json";
    private static final String LAPIS_DUST_TAG_PATH = "data/c/tags/item/dusts/lapis.json";

    private static JsonObject load(String path) throws IOException {
        try (InputStream stream = MarkerRecipeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Wiki claim (Crafting template): Lapis Dust over a Stick -> 1 Marker.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Crafting">wiki/Marker.txt § Crafting</a>
     */
    @Test
    @DisplayName("is Lapis Dust over a Stick, yielding one Marker")
    void matchesDocumentedShapeAndIngredients() throws IOException {
        JsonObject recipe = load(RECIPE_PATH);

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern).hasSize(2);
        assertThat(pattern.get(0).getAsString()).isEqualTo("L");
        assertThat(pattern.get(1).getAsString()).isEqualTo("S");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(RecipeJson.ingredient(key.get("L"))).isEqualTo("#c:dusts/lapis");
        assertThat(RecipeJson.ingredient(key.get("S"))).isEqualTo("minecraft:stick");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/marker");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }

    /**
     * Wiki claim (Crafting template): the top ingredient is our Lapis Dust. The recipe names the
     * {@code c:dusts/lapis} tag rather than the item, so the claim holds only while our dust is in it.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Marker#Crafting">wiki/Marker.txt § Crafting</a>
     */
    @Test
    @DisplayName("accepts our Lapis Dust through the c:dusts/lapis tag")
    void lapisDustTagContainsOurDust() throws IOException {
        List<String> values = new ArrayList<>();
        for (JsonElement value : load(LAPIS_DUST_TAG_PATH).getAsJsonArray("values")) {
            values.add(value.isJsonPrimitive() ? value.getAsString() : value.getAsJsonObject().get("id").getAsString());
        }

        assertThat(values).contains("logistics:core/lapis_dust");
    }
}
