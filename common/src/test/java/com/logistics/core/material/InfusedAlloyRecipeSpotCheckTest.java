package com.logistics.core.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
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
 * Pins each infused alloy to its shipped Transposer recipe, reading the bundled JSON rather than a
 * hand-built stand-in.
 *
 * <p>The line holds the dust and the fluid volume constant so that the <em>only</em> thing varying
 * across it is which fluid, which is what identifies the tier. Swapping a fluid here would silently
 * re-rank the energy ladder, since the alloy is the body material of both the cable and the battery
 * at that tier — so the pairing is pinned rather than left to a reviewer's eye.
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
        "caterium,logistics:core/gold_dust,logistics:core/liquid_redstone,1000,2000",
        "lumenite,logistics:core/amethyst_dust,logistics:core/liquid_glowstone,1000,3000",
        "echonite,logistics:core/echo_dust,logistics:core/liquid_ender,1000,4000"
    })
    @DisplayName("every alloy is one dust quenched in one bucket of its own fluid")
    void alloyMatchesItsInfusion(String alloy, String dust, String fluid, int milliBuckets, int energy)
            throws IOException {
        JsonObject recipe = loadRecipe(alloy);

        assertThat(recipe.get("type").getAsString()).isEqualTo("logistics:transposer");
        assertThat(ingredient(recipe.get("input"))).isEqualTo(dust);
        assertThat(recipe.get("energy").getAsInt()).isEqualTo(energy);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:core/" + alloy + "_ingot");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);

        JsonObject fluidSpec = recipe.getAsJsonObject("fluid");
        assertThat(fluidSpec.get("fluid").getAsString()).isEqualTo(fluid);
        // Negative drains the Transposer's tank — the Fill direction.
        assertThat(fluidSpec.get("amount").getAsInt()).isEqualTo(-milliBuckets);
    }

    /**
     * An ingredient in its canonical {@code #tag} / {@code item} spelling.
     *
     * <p>Vanilla accepts a bare string for an {@code Ingredient} on 26.x but demands an object on
     * 1.21.1, so the shipped JSON differs by branch while the recipe does not. Normalising here
     * keeps the assertions — and this file — the same across branches.
     */
    private static String ingredient(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        JsonObject object = element.getAsJsonObject();
        return object.has("tag") ? "#" + object.get("tag").getAsString() : object.get("item").getAsString();
    }
}
