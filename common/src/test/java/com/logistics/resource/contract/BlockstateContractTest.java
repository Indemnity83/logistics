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
 * Every model a blockstate names is shipped.
 *
 * <p>{@code variants} and {@code multipart} nest models at differing depths, and a variant may hold
 * a weighted list, so any string-valued "model" at any depth counts as a reference.
 */
@DisplayName("Blockstate contract")
class BlockstateContractTest {

    private static final Set<String> MODEL_KEYS = Set.of("model");

    @Test
    @DisplayName("every model a blockstate names is shipped")
    void everyBlockstateModelResolves() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("blockstates")) {
            JsonObject blockstate = ResourceFiles.parse(file);
            for (String reference : ResourceFiles.collectStrings(blockstate, MODEL_KEYS)) {
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

        assertThat(failures).as("unresolved blockstate models").isEmpty();
    }

    @Test
    @DisplayName("every blockstate names at least one model")
    void everyBlockstateNamesAModel() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFiles("blockstates")) {
            JsonObject blockstate = ResourceFiles.parse(file);
            if (ResourceFiles.collectStrings(blockstate, MODEL_KEYS).isEmpty()) {
                failures.add(ResourceFiles.describe(file) + " -> names no model at all");
            }
        }

        assertThat(failures).as("blockstates with no model reference").isEmpty();
    }

    /** Sanity guard, not a coverage claim: catches walking an empty or wrong directory. */
    @Test
    @DisplayName("the shipped blockstate set has not collapsed")
    void blockstateSetHasNotCollapsed() {
        assertThat(ResourceFiles.jsonFiles("blockstates"))
            .as("shipped blockstates; lower this floor deliberately if blocks were removed")
            .hasSizeGreaterThanOrEqualTo(70);
    }
}
