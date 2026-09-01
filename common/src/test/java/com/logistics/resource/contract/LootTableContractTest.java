package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every item a loot table drops is one we ship, matched against
 * {@link ResourceFiles#itemDefinitionIds()} — a proxy for the item registry, not proof of it.
 */
@DisplayName("Loot table contract")
class LootTableContractTest {

    private static final Set<String> ITEM_KEYS = Set.of("name");

    @Test
    @DisplayName("every item a loot table drops is shipped")
    void everyLootTableItemIsShipped() {
        Set<String> shipped = ResourceFiles.itemDefinitionIds();
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.dataJsonFiles(ResourceFiles.NAMESPACE + "/loot_table")) {
            for (String reference : ResourceFiles.collectStrings(ResourceFiles.parse(file), ITEM_KEYS)) {
                // "name" also labels non-item things (loot table references, predicates); only
                // namespaced ids we own are ours to verify.
                if (!reference.startsWith(ResourceFiles.NAMESPACE + ":")) {
                    continue;
                }
                if (!shipped.contains(reference)) {
                    failures.add(ResourceFiles.describe(file) + " -> drops unknown item '" + reference + "'");
                }
            }
        }

        assertThat(failures).as("loot tables dropping items we do not ship").isEmpty();
    }

    @Test
    @DisplayName("every loot table declares a type")
    void everyLootTableDeclaresAType() {
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.dataJsonFiles(ResourceFiles.NAMESPACE + "/loot_table")) {
            var table = ResourceFiles.parse(file);
            if (!table.has("type") || table.get("type").getAsString().isBlank()) {
                failures.add(ResourceFiles.describe(file) + " -> no loot table type");
            }
        }

        assertThat(failures).as("loot tables with no type").isEmpty();
    }

    /** Sanity guard, not a coverage claim: catches walking an empty or wrong directory. */
    @Test
    @DisplayName("the shipped loot table set has not collapsed")
    void lootTableSetHasNotCollapsed() {
        assertThat(ResourceFiles.dataJsonFiles(ResourceFiles.NAMESPACE + "/loot_table"))
            .as("shipped loot tables; lower this floor deliberately if blocks were removed")
            .hasSizeGreaterThanOrEqualTo(68);
    }
}
