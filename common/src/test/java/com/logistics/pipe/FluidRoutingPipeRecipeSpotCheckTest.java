package com.logistics.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.test.RecipeJson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Wiki claim (Crafting, on each of the Insertion, Merger, Bypass and Void Fluid Pipe pages): "Seal the
 * [item counterpart] with a slime ball or Tar (shapeless)".
 */
@DisplayName("Fluid-routing pipe recipes (spot check)")
class FluidRoutingPipeRecipeSpotCheckTest {

    private static JsonObject loadRecipe(String file) throws IOException {
        String path = "data/logistics/recipe/pipe/" + file + ".json";
        try (InputStream stream =
                FluidRoutingPipeRecipeSpotCheckTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "insertion_fluid_pipe,item_insertion_pipe,minecraft:slime_ball",
        "insertion_fluid_pipe_from_tar,item_insertion_pipe,logistics:core/tar",
        "merger_fluid_pipe,item_merger_pipe,minecraft:slime_ball",
        "merger_fluid_pipe_from_tar,item_merger_pipe,logistics:core/tar",
        "bypass_fluid_pipe,item_passthrough_pipe,minecraft:slime_ball",
        "bypass_fluid_pipe_from_tar,item_passthrough_pipe,logistics:core/tar",
        "void_fluid_pipe,item_void_pipe,minecraft:slime_ball",
        "void_fluid_pipe_from_tar,item_void_pipe,logistics:core/tar"
    })
    @DisplayName("each is its item counterpart sealed with one sealant, shapeless, one at a time")
    void sealedItemCounterpart(String file, String itemPipe, String sealant) throws IOException {
        JsonObject recipe = loadRecipe(file);

        assertThat(recipe.get("type").getAsString()).isEqualTo("minecraft:crafting_shapeless");
        List<String> ingredients = recipe.getAsJsonArray("ingredients").asList().stream()
                .map(RecipeJson::ingredient)
                .toList();
        assertThat(ingredients).containsExactlyInAnyOrder("logistics:pipe/" + itemPipe, sealant);

        JsonObject result = recipe.getAsJsonObject("result");
        String fluidPipe = file.replace("_from_tar", "");
        assertThat(result.get("id").getAsString()).isEqualTo("logistics:pipe/" + fluidPipe);
        assertThat(result.get("count").getAsInt()).isEqualTo(1);
    }
}
