package com.logistics.automation.crucible;

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
 * Spot-checks the Crucible's own crafting recipe and two representative melting recipes against the
 * bundled data.
 */
@DisplayName("Crucible recipes (spot check)")
class CrucibleRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String path) throws IOException {
        try (InputStream stream = CrucibleRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
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
     * Wiki claim (Crafting template): a 3x3 shaped recipe — row 1: _, Magma Block, _; row 2: Nether
     * Bricks, Machine Frame, Nether Bricks; row 3: Copper Gear, Redstone Reception Coil, Copper Gear
     * -> 1 Crucible.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Crafting">wiki/Crucible.txt § Crafting</a>
     */
    @Test
    @DisplayName("crafting recipe matches the wiki's documented shape and ingredients")
    void craftingRecipeMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/automation/crucible.json");

        var pattern = recipe.getAsJsonArray("pattern");
        assertThat(pattern.get(0).getAsString()).isEqualTo(" H ");
        assertThat(pattern.get(1).getAsString()).isEqualTo("NMN");
        assertThat(pattern.get(2).getAsString()).isEqualTo("GCG");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(ingredientId(key.getAsJsonObject("H"))).isEqualTo("minecraft:magma_block");
        assertThat(ingredientId(key.getAsJsonObject("N"))).isEqualTo("minecraft:nether_bricks");
        assertThat(ingredientId(key.getAsJsonObject("G"))).isEqualTo("logistics:core/copper_gear");
        assertThat(ingredientId(key.getAsJsonObject("C"))).isEqualTo("logistics:core/redstone_reception_coil");
        assertThat(ingredientId(key.getAsJsonObject("M"))).isEqualTo("logistics:core/machine_core"); // "Machine Frame" in-game

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:automation/crucible");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }

    /**
     * Wiki claim (Recipes § Lava & water): "Ice -> Water, Amount=1000."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Lava_.26_water">wiki/Crucible.txt § Lava & water</a>
     */
    @Test
    @DisplayName("ice melts into 1,000 mB of water")
    void iceMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/crucible/water_from_ice.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("minecraft:ice");
        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("fluid").getAsString()).isEqualTo("minecraft:water");
        assertThat(result.get("amount").getAsInt()).isEqualTo(1_000);
    }

    /**
     * Wiki claim (Recipes § Crude oil): "Bitumen -> Crude Oil, Amount=250."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Crude_oil">wiki/Crucible.txt § Crude oil</a>
     */
    @Test
    @DisplayName("bitumen melts into 250 mB of crude oil")
    void bitumenMatchesWiki() throws IOException {
        JsonObject recipe = loadRecipe("data/logistics/recipe/crucible/crude_oil_from_bitumen.json");

        assertThat(ingredientId(recipe.getAsJsonObject("ingredient"))).isEqualTo("logistics:core/bitumen");
        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("fluid").getAsString()).isEqualTo("logistics:core/crude_oil");
        assertThat(result.get("amount").getAsInt()).isEqualTo(250);
    }
}
