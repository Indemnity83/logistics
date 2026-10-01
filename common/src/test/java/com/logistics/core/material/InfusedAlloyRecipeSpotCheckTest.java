package com.logistics.core.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins the three infused alloys to their shipped Transposer recipes, reading the bundled JSON
 * rather than a hand-built stand-in.
 *
 * <p>Each alloy is a conductor quenched in a charged fluid, and the pairing is the whole point of
 * the line: swapping a fluid here would silently re-rank the energy ladder, since the alloy is the
 * body material of both the cable and the battery at that tier.
 */
@DisplayName("Infused alloy recipes (spot check)")
class InfusedAlloyRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String alloy) throws IOException {
        String path = "data/logistics/recipe/transposer/" + alloy + "_ingot.json";
        try (InputStream stream =
                InfusedAlloyRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "caterium,minecraft:gold_ingot,logistics:core/liquid_redstone,250,2000",
        "lumenite,logistics:core/amethyst_dust,logistics:core/liquid_glowstone,500,3000",
        "echonite,logistics:core/echo_dust,logistics:core/liquid_ender,750,4000"
    })
    @DisplayName("each alloy is its own conductor quenched in its own fluid")
    void alloyMatchesItsInfusion(String alloy, String input, String fluid, int milliBuckets, int energy)
            throws IOException {
        JsonObject recipe = loadRecipe(alloy);

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:transposer");
        assertThat(recipe.get("input").getAsString()).isEqualTo(input);
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(energy);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/" + alloy + "_ingot");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);

        JsonObject fluidSpec = recipe.getAsJsonObject("fluid");
        assertThat(fluidSpec.get("fluid").getAsString()).isEqualTo(fluid);
        // Negative drains the Transposer's tank — the Fill direction.
        assertThat(fluidSpec.get("amount").getAsInt()).isEqualTo(-milliBuckets);
    }
}
