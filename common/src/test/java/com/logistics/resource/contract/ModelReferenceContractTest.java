package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the shipped model graph holds together: every parent resolves, no parent chain loops,
 * and every texture a model names actually ships.
 *
 * <p>A broken reference here is invisible until the game loads the model and falls back to the
 * missing-texture checkerboard, which is exactly the class of bug that reaches players.
 */
@DisplayName("Model reference contract")
class ModelReferenceContractTest {

    private static final Set<String> TEXTURE_KEYS = Set.of("particle");

    @Test
    @DisplayName("every model parent resolves to a shipped model")
    void everyModelParentResolves() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("models")) {
            JsonObject model = ResourceFiles.parse(file);
            if (!model.has("parent")) {
                continue;
            }
            String parent = model.get("parent").getAsString();
            try {
                Path target = ResourceFiles.resolve(parent, "models", ".json");
                if (target != null && !Files.isRegularFile(target)) {
                    failures.add(ResourceFiles.describe(file) + " -> missing parent model '" + parent + "'");
                }
            } catch (ResourceFiles.UnexpectedNamespaceException e) {
                failures.add(ResourceFiles.describe(file) + " -> " + e.getMessage());
            }
        }

        assertThat(failures).as("unresolved model parents").isEmpty();
    }

    @Test
    @DisplayName("no model parent chain loops")
    void noModelParentChainLoops() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("models")) {
            Set<Path> seen = new LinkedHashSet<>();
            Path current = file;
            while (current != null && Files.isRegularFile(current)) {
                if (!seen.add(current)) {
                    failures.add(ResourceFiles.describe(file) + " -> parent chain loops: "
                        + seen.stream().map(ResourceFiles::describe).toList());
                    break;
                }
                JsonObject model = ResourceFiles.parse(current);
                if (!model.has("parent")) {
                    break;
                }
                current = ResourceFiles.resolve(model.get("parent").getAsString(), "models", ".json");
            }
        }

        assertThat(failures).as("looping model parent chains").isEmpty();
    }

    @Test
    @DisplayName("every texture a model names is shipped")
    void everyModelTextureResolves() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("models")) {
            JsonObject model = ResourceFiles.parse(file);
            for (String reference : texturesOf(model)) {
                // '#foo' points at another entry in the same map, resolved by the model loader.
                if (reference.startsWith("#")) {
                    continue;
                }
                try {
                    Path target = ResourceFiles.resolve(reference, "textures", ".png");
                    if (target != null && !Files.isRegularFile(target)) {
                        failures.add(ResourceFiles.describe(file) + " -> missing texture '" + reference + "'");
                    }
                } catch (ResourceFiles.UnexpectedNamespaceException e) {
                    failures.add(ResourceFiles.describe(file) + " -> " + e.getMessage());
                }
            }
        }

        assertThat(failures).as("unresolved model textures").isEmpty();
    }

    /**
     * Sanity guard, not a coverage claim: catches walking an empty or wrong directory. It says
     * nothing about whether the models that are there are correct.
     */
    @Test
    @DisplayName("the shipped model set has not collapsed")
    void modelSetHasNotCollapsed() {
        assertThat(ResourceFiles.jsonFiles("models"))
            .as("shipped models; lower this floor deliberately if models were removed")
            .hasSizeGreaterThanOrEqualTo(276);
    }

    /** Texture references live in a model's "textures" map, plus the top-level "particle" key. */
    private static List<String> texturesOf(JsonObject model) {
        List<String> references = new ArrayList<>();
        JsonElement textures = model.get("textures");
        if (textures != null && textures.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : textures.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    references.add(entry.getValue().getAsString());
                }
            }
        }
        references.addAll(ResourceFiles.collectStrings(model, TEXTURE_KEYS));
        return references;
    }
}
