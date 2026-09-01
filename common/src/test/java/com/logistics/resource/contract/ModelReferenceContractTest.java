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
 * Every model parent resolves, no parent chain loops, and every texture a model names ships.
 */
@DisplayName("Model reference contract")
class ModelReferenceContractTest {

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
            } catch (ResourceFiles.BadReferenceException e) {
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
                try {
                    current = ResourceFiles.resolve(model.get("parent").getAsString(), "models", ".json");
                } catch (ResourceFiles.BadReferenceException e) {
                    // everyModelParentResolves reports this; walking no further is enough here.
                    break;
                }
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
                } catch (ResourceFiles.BadReferenceException e) {
                    failures.add(ResourceFiles.describe(file) + " -> " + e.getMessage());
                }
            }
        }

        assertThat(failures).as("unresolved model textures").isEmpty();
    }

    /** Guards against walking an empty or wrong directory; says nothing about correctness. */
    @Test
    @DisplayName("the shipped model set has not collapsed")
    void modelSetHasNotCollapsed() {
        assertThat(ResourceFiles.jsonFiles("models"))
            .as("shipped models; lower this floor deliberately if models were removed")
            .hasSizeGreaterThanOrEqualTo(276);
    }

    /** Texture references live in a model's "textures" map, "particle" included. */
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
        return references;
    }
}
