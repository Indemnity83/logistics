package com.logistics.automation.refinery;

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
 * Checks the Refinery's two shipped recipes and its own crafting recipe against the bundled data.
 */
@DisplayName("Refinery recipes")
class RefineryRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream = RefineryRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
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
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Bucket, _; row 2: Glass Tank,
     * Machine Frame, Glass Tank; row 3: Copper Gear, Redstone Reception Coil, Copper Gear -> 1
     * Refinery.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Crafting">wiki/Refinery.txt § Crafting</a>
     */
    @Test
    @DisplayName("crafting recipe matches the wiki's documented shape and ingredients")
    void craftingRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/automation/refinery.json");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern.get(0).getAsString()).isEqualTo(" B ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("TMT");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(ingredientId(key.getAsJsonObject("B"))).isEqualTo("minecraft:bucket");
        assertThat(ingredientId(key.getAsJsonObject("T"))).isEqualTo("logistics:pipe/glass_tank");
        assertThat(ingredientId(key.getAsJsonObject("G"))).isEqualTo("logistics:core/copper_gear");
        assertThat(ingredientId(key.getAsJsonObject("C"))).isEqualTo("logistics:core/redstone_reception_coil");
        assertThat(ingredientId(key.getAsJsonObject("M"))).isEqualTo("logistics:core/machine_core"); // "Machine Frame" in-game

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:automation/refinery");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }

    /**
     * Wiki claim (Recipes): "Liquid Biomass (200 mB) -> Bio Fuel (100 mB)."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Recipes">wiki/Refinery.txt § Recipes</a>
     */
    @Test
    @DisplayName("liquid biomass distills into bio fuel at the documented amounts and RF cost")
    void bioFuelRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/refinery/bio_fuel_from_biomass.json");

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:refinery");

        JsonObject input = recipe.getAsJsonObject("input");
        assertThat(input.get("fluid").getAsString()).isEqualTo("logistics:core/liquid_biomass");
        assertThat(input.get("amount").getAsInt()).isEqualTo(200);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("fluid").getAsString()).isEqualTo("logistics:core/bio_fuel");
        assertThat(result.get("amount").getAsInt()).isEqualTo(100);

        assertThat(recipe.get("energy").getAsInt()).isEqualTo(5_000);
        assertThat(recipe.has("byproduct")).isFalse();
    }

    /**
     * Wiki claim (Recipes): "Crude Oil (200 mB) -> Fuel Oil (150 mB), Byproduct: Tar, 50% chance."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Recipes">wiki/Refinery.txt § Recipes</a>
     */
    @Test
    @DisplayName("crude oil distills into fuel oil with a 50% tar byproduct at the documented amounts")
    void fuelOilRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/refinery/fuel_oil_from_crude_oil.json");

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:refinery");

        JsonObject input = recipe.getAsJsonObject("input");
        assertThat(input.get("fluid").getAsString()).isEqualTo("logistics:core/crude_oil");
        assertThat(input.get("amount").getAsInt()).isEqualTo(200);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("fluid").getAsString()).isEqualTo("logistics:core/fuel_oil");
        assertThat(result.get("amount").getAsInt()).isEqualTo(150);

        assertThat(recipe.get("energy").getAsInt()).isEqualTo(5_000);

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("logistics:core/tar");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(0.5);
    }
}
