package com.logistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Structural smoke test over every recipe JSON file shipped in {@code common}'s resources: parses
 * each as JSON and checks the shape common to all recipe types here (a non-blank {@code type}, and
 * a well-formed {@code result} where present), without resolving ingredients through Minecraft's
 * item registry. Catches JSON syntax errors, a missing/blank {@code type}, and a malformed
 * {@code result} block — the most common ways a hand-edited recipe silently breaks (see
 * WIKI_DISCREPANCIES.md § Kiln for the mismatch that motivated this test).
 */
@DisplayName("Recipe JSON files")
class RecipeJsonSmokeTest {

    private static final String RECIPE_ROOT = "data/logistics/recipe";

    private static boolean hasNonBlankString(JsonObject object, String field) {
        return object.has(field)
                && object.get(field).isJsonPrimitive()
                && object.get(field).getAsJsonPrimitive().isString()
                && !object.get(field).getAsString().isBlank();
    }

    private static List<Path> recipeFiles() throws IOException, URISyntaxException {
        URL rootUrl = RecipeJsonSmokeTest.class.getClassLoader().getResource(RECIPE_ROOT);
        assertThat(rootUrl).as("recipe resource root on the test classpath: %s", RECIPE_ROOT).isNotNull();
        Path root = Paths.get(rootUrl.toURI());
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }
    }

    @Test
    @DisplayName("all parse as JSON, and each has a non-blank type and well-formed result")
    void everyRecipeFileIsStructurallyValid() throws IOException, URISyntaxException {
        List<Path> files = recipeFiles();
        assertThat(files).as("recipe JSON files under " + RECIPE_ROOT).isNotEmpty();

        for (Path file : files) {
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(Files.readString(file));
            } catch (JsonParseException e) {
                fail("%s is not valid JSON: %s", file, e.getMessage());
                continue;
            }

            if (!parsed.isJsonObject()) {
                fail("%s: top level is not a JSON object", file);
                continue;
            }
            JsonObject recipe = parsed.getAsJsonObject();

            assertThat(hasNonBlankString(recipe, "type"))
                    .as("%s has a non-blank string 'type'", file)
                    .isTrue();

            if (recipe.has("result")) {
                JsonElement result = recipe.get("result");
                // Every domain here uses a plain "minecraft:item" string, an item result
                // { "id": ..., "count": ... }, or a fluid result { "fluid": ..., "amount": ... }
                // (Crucible/Refinery) — never a bare number, array, or null.
                boolean validShape = (result.isJsonPrimitive() && result.getAsJsonPrimitive().isString())
                        || (result.isJsonObject() && hasNonBlankString(result.getAsJsonObject(), "id"))
                        || (result.isJsonObject() && hasNonBlankString(result.getAsJsonObject(), "fluid"));
                assertThat(validShape)
                        .as("%s has a well-formed 'result' (string id, {id, count}, or {fluid, amount})", file)
                        .isTrue();
            }
        }
    }
}
