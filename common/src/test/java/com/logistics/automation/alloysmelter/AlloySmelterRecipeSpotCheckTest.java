package com.logistics.automation.alloysmelter;

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
 * Spot-checks the Alloy Smelter's own crafting recipe and two representative processing recipes
 * against the bundled data.
 */
@DisplayName("Alloy Smelter recipes (spot check)")
class AlloySmelterRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream = AlloySmelterRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
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
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Diamond Gear, _; row 2: Sand,
     * Machine Frame, Sand; row 3: Copper Gear, Redstone Reception Coil, Copper Gear -> 1 Alloy
     * Smelter. The sand key accepts either Sand or Red Sand.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Crafting">wiki/Alloy Smelter.txt § Crafting</a>
     */
    @Test
    @DisplayName("crafting recipe matches the wiki's documented shape and ingredients")
    void craftingRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/automation/alloy_smelter.json");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern.get(0).getAsString()).isEqualTo(" D ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("SMS");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(ingredientId(key.getAsJsonObject("D"))).isEqualTo("logistics:core/diamond_gear");
        assertThat(ingredientId(key.getAsJsonObject("G"))).isEqualTo("logistics:core/copper_gear");
        assertThat(ingredientId(key.getAsJsonObject("C"))).isEqualTo("logistics:core/redstone_reception_coil");
        assertThat(ingredientId(key.getAsJsonObject("M"))).isEqualTo("logistics:core/machine_core"); // "Machine Frame" in-game

        JsonArray sandOptions = key.getAsJsonArray("S");
        assertThat(sandOptions).hasSize(2);
        assertThat(ingredientId(sandOptions.get(0).getAsJsonObject())).isEqualTo("minecraft:sand");
        assertThat(ingredientId(sandOptions.get(1).getAsJsonObject())).isEqualTo("minecraft:red_sand");
    }

    /**
     * Wiki claim (Ore processing): "Iron Ore;Deepslate Iron Ore + Sand;Red Sand -> Iron Ingot,2,
     * Byproduct: Rich Slag, 5% chance."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Ore_processing">wiki/Alloy Smelter.txt § Ore processing</a>
     */
    @Test
    @DisplayName("iron ore + sand smelts into 2 iron ingots with a 5% rich slag byproduct")
    void ironOreMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/alloy_smelter/iron_from_ore.json");

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertThat(ingredients).hasSize(2);
        assertThat(ingredientId(ingredients.get(0).getAsJsonObject())).isEqualTo("#c:ores/iron");
        assertThat(ingredientId(ingredients.get(1).getAsJsonObject())).isEqualTo("#c:sands");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("minecraft:iron_ingot");
        assertThat(result.get("count").getAsInt()).isEqualTo(2);

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("logistics:core/rich_slag");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(0.05);
    }

    /**
     * Wiki claim (Alloying): "Bronze is smelted from three copper and one tin, in ingot...form" —
     * "Copper Ingot,InputCount=3 + Tin Ingot -> Bronze Ingot,4."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Alloying">wiki/Alloy Smelter.txt § Alloying</a>
     */
    @Test
    @DisplayName("3 copper ingots + 1 tin ingot alloy into 4 bronze ingots with no byproduct")
    void bronzeAlloyMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/alloy_smelter/bronze_from_copper_ingot_tin_ingot.json");

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertThat(ingredients).hasSize(2);
        JsonObject copper = ingredients.get(0).getAsJsonObject();
        assertThat(ingredientId(copper.getAsJsonObject("id"))).isEqualTo("minecraft:copper_ingot");
        assertThat(copper.get("count").getAsInt()).isEqualTo(3);
        assertThat(ingredientId(ingredients.get(1).getAsJsonObject())).isEqualTo("#c:ingots/tin");

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/bronze_ingot");
        assertThat(result.get("count").getAsInt()).isEqualTo(4);
        assertThat(recipe.has("byproduct")).isFalse();
    }
}
