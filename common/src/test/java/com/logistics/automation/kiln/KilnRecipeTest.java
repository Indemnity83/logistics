package com.logistics.automation.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the Kiln's actual shipped crafting recipe against what the wiki documents, reading the
 * bundled JSON directly rather than a hand-built stand-in — the gap that let a real "Machine Frame"
 * vs. "Machine Core" mismatch ship unnoticed (see WIKI_DISCREPANCIES.md § Kiln). No live
 * {@code RecipeManager}/datapack reload is needed to decode one known file, so this stays plain
 * JUnit rather than a GameTest.
 */
@DisplayName("Kiln crafting recipe")
class KilnRecipeTest {

    private static final String RECIPE_PATH = "data/logistics/recipe/automation/kiln.json";

    private static JsonObject loadKilnRecipe() throws IOException {
        try (InputStream stream = KilnRecipeTest.class.getClassLoader().getResourceAsStream(RECIPE_PATH)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", RECIPE_PATH).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Redstone, _; row 2: Bricks,
     * Machine Frame, Bricks; row 3: Copper Gear, Redstone Reception Coil, Copper Gear -> 1 Kiln.
     *
     * <p>NOTE: the wiki names the row-2 center ingredient "Machine Frame". The recipe actually keys
     * it to {@code logistics:core/machine_core} ("Machine Core") — a distinct item. This test
     * asserts the shipped recipe as-is; see WIKI_DISCREPANCIES.md § Kiln for the tracked mismatch.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Crafting">wiki/Kiln.txt § Crafting</a>
     */
    @Test
    @DisplayName("matches the wiki's documented shape and ingredients, except the row-2 center item")
    void matchesDocumentedShapeAndIngredients() throws IOException {
        JsonObject recipe = loadKilnRecipe();

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern).hasSize(3);
        assertThat(pattern.get(0).getAsString()).isEqualTo(" R ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("BMB");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(key.get("R").getAsString()).isEqualTo("minecraft:redstone");
        assertThat(key.get("B").getAsString()).isEqualTo("minecraft:bricks");
        assertThat(key.get("G").getAsString()).isEqualTo("logistics:core/copper_gear");
        assertThat(key.get("C").getAsString()).isEqualTo("logistics:core/redstone_reception_coil");
        // NOTE: wiki says "Machine Frame"; the shipped recipe uses Machine Core. See above.
        assertThat(key.get("M").getAsString()).isEqualTo("logistics:core/machine_core");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:automation/kiln");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }
}
