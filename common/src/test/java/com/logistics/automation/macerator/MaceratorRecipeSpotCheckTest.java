package com.logistics.automation.macerator;

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
 * Spot-checks one representative shipped Macerator recipe against the wiki's grinding table,
 * reading the bundled JSON directly rather than a hand-built stand-in.
 */
@DisplayName("Macerator recipe (spot check)")
class MaceratorRecipeSpotCheckTest {

    private static final String RECIPE_PATH = "data/logistics/recipe/macerator/iron_dust.json";

    private static JsonObject loadRecipe() throws IOException {
        try (InputStream stream =
                MaceratorRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(RECIPE_PATH)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", RECIPE_PATH).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /**
     * Wiki claim (Recipes § Ores → Dust): "Iron Ore;Deepslate Iron Ore -> Iron Dust,2 | Byproduct:
     * Tin Dust | ByproductChance: 10%" and (Usage): "Metal ores also have a 10% chance of a bonus
     * dust on the side" / "most ores take 2,000 RF (10 seconds)."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Macerator#Ores_.E2.86.92_Dust">wiki/Macerator.txt § Ores → Dust</a>
     */
    @Test
    @DisplayName("iron ore grinds into 2 dust with a 10% tin dust byproduct at 2,000 RF")
    void ironOreMatchesWikiGrindingTable() throws IOException {
        JsonObject recipe = loadRecipe();

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:macerator");
        assertThat(recipe.get("ingredient").getAsString()).isEqualTo("minecraft:iron_ore");
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(2_000);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/iron_dust");
        assertThat(result.get("count").getAsInt()).isEqualTo(2);

        JsonObject byproduct = recipe.getAsJsonObject("byproduct");
        assertThat(byproduct.get("id").getAsString()).isEqualTo("logistics:core/tin_dust");
        assertThat(byproduct.get("chance").getAsDouble()).isEqualTo(0.1);
    }
}
