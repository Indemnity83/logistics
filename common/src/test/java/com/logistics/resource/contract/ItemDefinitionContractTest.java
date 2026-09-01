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
 * Proves every shipped item definition points at a model that exists.
 *
 * <p>Item definitions nest models in several shapes — {@code minecraft:model} names one directly,
 * {@code minecraft:composite} lists several, {@code minecraft:select} nests them under a fallback
 * and per-case branches, and {@code minecraft:special} carries its rendered model under
 * {@code base}. Rather than encode each type's schema, this walks the whole tree and treats any
 * string-valued "model" or "base" as a reference, so a new type is covered without a code change.
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
                } catch (ResourceFiles.UnexpectedNamespaceException e) {
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

    /**
     * Sanity guard, not a coverage claim: catches walking an empty or wrong directory.
     */
    @Test
    @DisplayName("the shipped item definition set has not collapsed")
    void itemDefinitionSetHasNotCollapsed() {
        assertThat(ResourceFiles.jsonFiles("items"))
            .as("shipped item definitions; lower this floor deliberately if items were removed")
            .hasSizeGreaterThanOrEqualTo(184);
    }
}
