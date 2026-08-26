package com.logistics.automation.transposer;

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
 * Spot-checks the Transposer's own crafting recipe and its two water-bucket conversion recipes
 * against the wiki, reading the bundled JSON directly. The Transposer has 48 conversion recipes
 * ({@code com.logistics.RecipeJsonSmokeTest} covers all of them structurally); these are content
 * checks in the same spirit as {@code KilnRecipeTest}, not exhaustive.
 */
@DisplayName("Transposer recipes (spot check)")
class TransposerRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream = TransposerRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Bucket, _; row 2: Glass,
     * Machine Frame, Glass; row 3: Copper Gear, Redstone Reception Coil, Copper Gear -> 1 Transposer.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Crafting">wiki/Transposer.txt § Crafting</a>
     */
    @Test
    @DisplayName("crafting recipe matches the wiki's documented shape and ingredients")
    void craftingRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/automation/transposer.json");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern.get(0).getAsString()).isEqualTo(" B ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("SMS");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(key.get("B").getAsString()).isEqualTo("minecraft:bucket");
        assertThat(key.get("S").getAsString()).isEqualTo("minecraft:glass");
        assertThat(key.get("G").getAsString()).isEqualTo("logistics:core/copper_gear");
        assertThat(key.get("C").getAsString()).isEqualTo("logistics:core/redstone_reception_coil");
        // "Machine Frame" in-game; logistics:core/machine_core is its (unchanged) registry id.
        assertThat(key.get("M").getAsString()).isEqualTo("logistics:core/machine_core");
    }

    /**
     * Wiki claim (Usage): "An empty bucket plus at least 1,000 mB in the tank becomes a filled
     * bucket of that fluid; the tank loses 1,000 mB." (Power): "A bucket fill/empty costs 800 RF."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @Test
    @DisplayName("filling a water bucket costs 800 RF and drains 1,000 mB")
    void fillWaterBucketMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/transposer/fill_water_bucket.json");

        assertThat(recipe.get("input").getAsString()).isEqualTo("minecraft:bucket");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(800);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("minecraft:water_bucket");

        JsonObject fluid = recipe.getAsJsonObject("fluid");
        assertThat(fluid.get("fluid").getAsString()).isEqualTo("minecraft:water");
        assertThat(fluid.get("amount").getAsInt()).isEqualTo(-1000); // negative = drained from the tank
    }

    /**
     * Wiki claim (Usage): "A filled bucket...plus room for 1,000 mB becomes a plain empty bucket;
     * the tank gains 1,000 mB."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Usage">wiki/Transposer.txt § Usage</a>
     */
    @Test
    @DisplayName("emptying a water bucket costs 800 RF and fills the tank by 1,000 mB")
    void emptyWaterBucketMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/transposer/empty_water_bucket.json");

        assertThat(recipe.get("input").getAsString()).isEqualTo("minecraft:water_bucket");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(800);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("minecraft:bucket");

        JsonObject fluid = recipe.getAsJsonObject("fluid");
        assertThat(fluid.get("fluid").getAsString()).isEqualTo("minecraft:water");
        assertThat(fluid.get("amount").getAsInt()).isEqualTo(1_000); // positive = added to the tank
    }
}
