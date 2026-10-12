package com.logistics.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.test.RecipeJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts the Crafting, Process and Satellite Logistics Pipes' shipped crafting recipes against
 * what their wiki pages document, reading the bundled JSON directly.
 */
@DisplayName("Crafting / Process / Satellite pipe crafting recipes")
class CraftingProcessSatellitePipeRecipeTest {

    private static final String BASIC_PIPE = "logistics:pipe/basic_logistics_pipe";

    private static JsonObject load(String file) throws IOException {
        String path = "data/logistics/recipe/pipe/" + file;
        try (InputStream stream =
                CraftingProcessSatellitePipeRecipeTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Asserts the shared " A " / "BCB" shape: gear-or-chipset on top, the side item either side of a Basic Logistics Pipe. */
    private static void assertGearOverPipeRecipe(JsonObject recipe, String top, String sides, String result) {
        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern).hasSize(2);
        assertThat(pattern.get(0).getAsString()).isEqualTo(" A ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("BCB");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(RecipeJson.ingredient(key.get("A"))).isEqualTo(top);
        assertThat(RecipeJson.ingredient(key.get("B"))).isEqualTo(sides);
        assertThat(RecipeJson.ingredient(key.get("C"))).isEqualTo(BASIC_PIPE);

        assertResult(recipe, result);
    }

    private static void assertResult(JsonObject recipe, String result) {
        JsonObject output = recipe.getAsJsonObject("result");
        assertThat(output.get("id").getAsString()).isEqualTo(result);
        assertThat(output.has("count") ? output.get("count").getAsInt() : 1).isEqualTo(1);
    }

    /**
     * Wiki claim (Crafting template): row 1: _, Iron Gear, _; row 2: Amethyst Dust, Basic Logistics
     * Pipe, Amethyst Dust -> 1 Crafting Logistics Pipe. "Substitute an Iron Chipset for the Iron
     * Gear".
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crafting_Logistics_Pipe#Crafting">wiki/Crafting Logistics Pipe.txt § Crafting</a>
     */
    @Test
    @DisplayName("Crafting Logistics Pipe matches the wiki, with gear and chipset variants")
    void craftingLogisticsPipeMatchesWiki() throws IOException {
        String result = "logistics:pipe/crafting_logistics_pipe";
        assertGearOverPipeRecipe(load("crafting_logistics_pipe.json"), "#c:gears/iron", "#c:dusts/amethyst", result);
        assertGearOverPipeRecipe(
                load("crafting_logistics_pipe_from_chipset.json"),
                "logistics:core/iron_chipset",
                "#c:dusts/amethyst",
                result);
    }

    /**
     * Wiki claim (Crafting template): row 1: _, Iron Gear, _; row 2: Redstone, Basic Logistics
     * Pipe, Redstone -> 1 Process Logistics Pipe. "Substitute an Iron Chipset for the Iron Gear".
     *
     * @see <a href="https://logistics.fandom.com/wiki/Process_Logistics_Pipe#Crafting">wiki/Process Logistics Pipe.txt § Crafting</a>
     */
    @Test
    @DisplayName("Process Logistics Pipe matches the wiki, with gear and chipset variants")
    void processLogisticsPipeMatchesWiki() throws IOException {
        String result = "logistics:pipe/process_logistics_pipe";
        assertGearOverPipeRecipe(load("process_logistics_pipe.json"), "#c:gears/iron", "minecraft:redstone", result);
        assertGearOverPipeRecipe(
                load("process_logistics_pipe_from_chipset.json"),
                "logistics:core/iron_chipset",
                "minecraft:redstone",
                result);
    }

    /**
     * Wiki claim (Crafting template): row 1: Redstone, Basic Logistics Pipe, Redstone -> 1
     * Satellite Logistics Pipe.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Satellite_Logistics_Pipe#Crafting">wiki/Satellite Logistics Pipe.txt § Crafting</a>
     */
    @Test
    @DisplayName("Satellite Logistics Pipe matches the wiki")
    void satelliteLogisticsPipeMatchesWiki() throws IOException {
        JsonObject recipe = load("satellite_logistics_pipe.json");

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern).hasSize(1);
        assertThat(pattern.get(0).getAsString()).isEqualTo("ABA");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(RecipeJson.ingredient(key.get("A"))).isEqualTo("minecraft:redstone");
        assertThat(RecipeJson.ingredient(key.get("B"))).isEqualTo(BASIC_PIPE);

        assertResult(recipe, "logistics:pipe/satellite_logistics_pipe");
    }
}
