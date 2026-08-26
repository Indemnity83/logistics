package com.logistics.automation.pump;

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
 * Asserts the Pump's actual shipped crafting recipe against what the wiki documents, reading the
 * bundled JSON directly rather than a hand-built stand-in.
 */
@DisplayName("Fluid Pump crafting recipe")
class FluidPumpRecipeTest {

    private static final String RECIPE_PATH = "data/logistics/recipe/automation/fluid_pump.json";

    private static JsonObject loadRecipe() throws IOException {
        try (InputStream stream = FluidPumpRecipeTest.class.getClassLoader().getResourceAsStream(RECIPE_PATH)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", RECIPE_PATH).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Glass Tank, _; row 2: Bucket,
     * Machine Frame, Bucket; row 3: Copper Gear, Redstone Reception Coil, Copper Gear -> 1 Pump.
     *
     * <p>The row-2 center key resolves to item id {@code logistics:core/machine_core}, the same
     * stable id behind "Machine Frame" verified on the Kiln (see KilnRecipeTest / WIKI_DISCREPANCIES.md
     * § Kiln) — not a mismatch here either.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Crafting">wiki/Pump.txt § Crafting</a>
     */
    @Test
    @DisplayName("matches the wiki's documented shape and ingredients")
    void matchesDocumentedShapeAndIngredients() throws IOException {
        JsonObject recipe = loadRecipe();

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern).hasSize(3);
        assertThat(pattern.get(0).getAsString()).isEqualTo(" T ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("BMB");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(key.get("T").getAsString()).isEqualTo("logistics:pipe/glass_tank");
        assertThat(key.get("B").getAsString()).isEqualTo("minecraft:bucket");
        assertThat(key.get("G").getAsString()).isEqualTo("logistics:core/copper_gear");
        assertThat(key.get("C").getAsString()).isEqualTo("logistics:core/redstone_reception_coil");
        // "Machine Frame" in-game; logistics:core/machine_core is its (unchanged) registry id.
        assertThat(key.get("M").getAsString()).isEqualTo("logistics:core/machine_core");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:automation/fluid_pump");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }
}
