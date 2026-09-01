package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every shipped item definition points at a model that exists.
 *
 * <p>Definitions nest models at varying depths by type ({@code model}, {@code composite},
 * {@code select}, {@code special}), so any string-valued "model" or "base" at any depth counts
 * as a reference rather than each schema being encoded.
 */
@DisplayName("Item definition contract")
class ItemDefinitionContractTest {

    private static final Set<String> MODEL_KEYS = Set.of("model", "base");

    @Test
    @DisplayName("every model an item definition names is shipped")
    void everyItemDefinitionModelResolves() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("items")) {
            JsonObject definition = ResourceFiles.parse(file);
            for (String reference : ResourceFiles.collectStrings(definition, MODEL_KEYS)) {
                try {
                    Path target = ResourceFiles.resolve(reference, "models", ".json");
                    if (target != null && !Files.isRegularFile(target)) {
                        failures.add(ResourceFiles.describe(file) + " -> missing model '" + reference + "'");
                    }
                } catch (ResourceFiles.BadReferenceException e) {
                    failures.add(ResourceFiles.describe(file) + " -> " + e.getMessage());
                }
            }
        }

        assertThat(failures).as("unresolved item definition models").isEmpty();
    }

    @Test
    @DisplayName("every item definition names at least one model")
    void everyItemDefinitionNamesAModel() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("items")) {
            JsonObject definition = ResourceFiles.parse(file);
            if (ResourceFiles.collectStrings(definition, MODEL_KEYS).isEmpty()) {
                failures.add(ResourceFiles.describe(file) + " -> names no model at all");
            }
        }

        assertThat(failures).as("item definitions with no model reference").isEmpty();
    }

    /** Guards against walking an empty or wrong directory; says nothing about correctness. */
    @Test
    @DisplayName("the shipped item definition set has not collapsed")
    void itemDefinitionSetHasNotCollapsed() {
        assertThat(ResourceFiles.jsonFiles("items"))
            .as("shipped item definitions; lower this floor deliberately if items were removed")
            .hasSizeGreaterThanOrEqualTo(184);
    }
}
