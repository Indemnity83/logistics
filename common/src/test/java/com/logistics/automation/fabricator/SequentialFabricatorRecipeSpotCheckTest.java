package com.logistics.automation.fabricator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spot-checks the Sequential Fabricator's own crafting recipe plus its lowest- and highest-tier
 * chipset recipes against the bundled data.
 */
@DisplayName("Sequential Fabricator recipes (spot check)")
class SequentialFabricatorRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream =
                SequentialFabricatorRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** This branch's vanilla Ingredient JSON is always {@code {"item": ...}} or {@code {"tag": ...}}. */
    private static String ingredientId(JsonObject ingredient) {
        return ingredient.has("tag")
                ? "#" + ingredient.get("tag").getAsString()
                : ingredient.get("item").getAsString();
    }

    /**
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Netherite Gear, _; row 2:
     * Obsidian, Machine Frame, Obsidian; row 3: Copper Gear, Redstone Reception Coil, Copper Gear ->
     * 1 Sequential Fabricator.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Crafting">wiki/Sequential Fabricator.txt § Crafting</a>
     */
    @Test
    @DisplayName("crafting recipe matches the wiki's documented shape and ingredients")
    void craftingRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/automation/sequential_fabricator.json");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern.get(0).getAsString()).isEqualTo(" N ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("OMO");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(ingredientId(key.getAsJsonObject("N"))).isEqualTo("logistics:core/netherite_gear");
        assertThat(ingredientId(key.getAsJsonObject("O"))).isEqualTo("minecraft:obsidian");
        assertThat(ingredientId(key.getAsJsonObject("G"))).isEqualTo("logistics:core/copper_gear");
        assertThat(ingredientId(key.getAsJsonObject("C"))).isEqualTo("logistics:core/redstone_reception_coil");
        assertThat(ingredientId(key.getAsJsonObject("M"))).isEqualTo("logistics:core/machine_core"); // "Machine Frame" in-game

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:automation/sequential_fabricator");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }

    /**
     * Wiki claim (Recipes): "Redstone -> Redstone Chipset, 10,000 RF" — the cheapest chipset, and
     * the only one needing just a single ingredient.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Recipes">wiki/Sequential Fabricator.txt § Recipes</a>
     */
    @Test
    @DisplayName("redstone chipset needs only redstone at 10,000 RF")
    void redstoneChipsetMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/fabricator/redstone_chipset.json");

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertThat(ingredients).hasSize(1);
        JsonObject ingredient = ingredients.get(0).getAsJsonObject();
        assertThat(ingredientId(ingredient.getAsJsonObject("ingredient"))).isEqualTo("minecraft:redstone");
        assertThat(ingredient.get("count").getAsInt()).isEqualTo(1);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/redstone_chipset");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(10_000);
    }

    /**
     * Wiki claim (Recipes): "Redstone + Netherite Ingot -> Netherite Chipset, 120,000 RF" — the most
     * expensive chipset.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Recipes">wiki/Sequential Fabricator.txt § Recipes</a>
     */
    @Test
    @DisplayName("netherite chipset needs redstone and a netherite ingot at 120,000 RF")
    void netheriteChipsetMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/fabricator/netherite_chipset.json");

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertThat(ingredients).hasSize(2);
        assertThat(ingredientId(ingredients.get(0).getAsJsonObject().getAsJsonObject("ingredient")))
                .isEqualTo("minecraft:redstone");
        assertThat(ingredientId(ingredients.get(1).getAsJsonObject().getAsJsonObject("ingredient")))
                .isEqualTo("minecraft:netherite_ingot");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/netherite_chipset");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(120_000);
    }
}
