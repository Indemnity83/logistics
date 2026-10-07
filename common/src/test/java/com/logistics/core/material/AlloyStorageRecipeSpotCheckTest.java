package com.logistics.core.material;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.logistics.test.RecipeJson;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pins the 9↔1 compression round trip for each alloy, so an ingot survives a trip through its
 * storage block and through nuggets without gaining or losing material.
 */
@DisplayName("Alloy storage recipes (spot check)")
class AlloyStorageRecipeSpotCheckTest {

    private static JsonObject load(String name) throws IOException {
        String path = "data/logistics/recipe/core/" + name + ".json";
        try (InputStream stream = AlloyStorageRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<String> pattern(JsonObject recipe) {
        return recipe.getAsJsonArray("pattern").asList().stream().map(e -> e.getAsString()).toList();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"caterium", "lumenite", "echonite"})
    @DisplayName("nine ingots make a block and the block gives nine back")
    void blockRoundTrips(String alloy) throws IOException {
        JsonObject pack = load(alloy + "_block");
        assertThat(pattern(pack)).containsExactly("BBB", "BBB", "BBB");
        assertThat(RecipeJson.ingredient(pack.getAsJsonObject("key").get("B"))).isEqualTo("#c:ingots/" + alloy);
        assertThat(pack.getAsJsonObject("result").get("id").getAsString())
                .isEqualTo("logistics:core/" + alloy + "_block");
        assertThat(pack.getAsJsonObject("result").get("count").getAsInt()).isEqualTo(1);

        JsonObject unpack = load(alloy + "_ingot_from_block");
        assertThat(RecipeJson.ingredient(unpack.getAsJsonArray("ingredients").get(0)))
                .isEqualTo("#c:storage_blocks/" + alloy);
        assertThat(unpack.getAsJsonObject("result").get("count").getAsInt()).isEqualTo(9);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"caterium", "lumenite", "echonite"})
    @DisplayName("one ingot makes nine nuggets and nine nuggets give it back")
    void nuggetRoundTrips(String alloy) throws IOException {
        JsonObject split = load(alloy + "_nugget");
        assertThat(RecipeJson.ingredient(split.getAsJsonArray("ingredients").get(0))).isEqualTo("#c:ingots/" + alloy);
        assertThat(split.getAsJsonObject("result").get("count").getAsInt()).isEqualTo(9);

        JsonObject merge = load(alloy + "_ingot_from_nuggets");
        assertThat(pattern(merge)).containsExactly("NNN", "NNN", "NNN");
        assertThat(RecipeJson.ingredient(merge.getAsJsonObject("key").get("N"))).isEqualTo("#c:nuggets/" + alloy);
        assertThat(merge.getAsJsonObject("result").get("count").getAsInt()).isEqualTo(1);
    }
}
