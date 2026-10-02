package com.logistics.power.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins every battery tier to one recipe silhouette — clay packed around a block of the tier's own
 * storage material, with two nuggets of its conductor as terminals.
 *
 * <p>The core and the terminals are what identify the tier, so they are asserted per tier rather
 * than left to a reviewer's eye: swapping Lumenite for Echonite here would silently reorder the
 * ladder while every other file still looked correct.
 *
 * <p>Basic is deliberately the odd one out. Its core is a Redstone Block and its terminals are
 * copper — <em>store</em> versus <em>transmit</em> — because redstone is the storage material at
 * that rank and has no nugget form. Every tier above it is built from one alloy doing both jobs.
 */
@DisplayName("Battery recipes (spot check)")
class BatteryRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String tier) throws IOException {
        String path = "data/logistics/recipe/power/" + tier + "_battery.json";
        try (InputStream stream = BatteryRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "basic,minecraft:redstone_block,logistics:core/copper_nugget",
        "conductive,logistics:core/caterium_block,logistics:core/caterium_nugget",
        "resonant,logistics:core/lumenite_block,logistics:core/lumenite_nugget",
        "deep,logistics:core/echonite_block,logistics:core/echonite_nugget"
    })
    @DisplayName("every tier is clay around its own core, with its own nuggets as terminals")
    void batteryMatchesTheSharedSilhouette(String tier, String core, String terminal) throws IOException {
        JsonObject recipe = loadRecipe(tier);

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shaped");

        List<String> pattern =
                recipe.getAsJsonArray("pattern").asList().stream().map(e -> e.getAsString()).toList();
        assertThat(pattern).containsExactly("NCN", "CBC", "CCC");

        JsonObject key = recipe.getAsJsonObject("key");
        assertThat(key.get("C").getAsString()).as("housing is always clay").isEqualTo("minecraft:clay_ball");
        assertThat(key.get("B").getAsString()).as("core is the tier's storage material").isEqualTo(core);
        assertThat(key.get("N").getAsString()).as("terminals are the tier's conductor").isEqualTo(terminal);

        JsonObject result = recipe.getAsJsonObject("result");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:power/" + tier + "_battery");
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }
}
