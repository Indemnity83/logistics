package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Our gears are interchangeable with other mods' gears: a recipe asks for the conventional
 * {@code c:gears/<material>} tag, never our gear item.
 */
@DisplayName("Gear tag contract")
class GearTagContractTest {

    /** Recipe keys holding what a recipe produces; everything else a recipe names is an input. */
    private static final Set<String> OUTPUT_KEYS = Set.of("result", "byproduct");

    @Test
    @DisplayName("no recipe asks for one of our gears by exact id")
    void recipesAskForGearTags() {
        Map<String, String> tagOf = gearTags();
        assertThat(tagOf).as("our gears in c:gears tags; the tag scan found nothing").isNotEmpty();

        List<String> failures = new ArrayList<>();
        for (Path file : ResourceFiles.dataJsonFiles("logistics/recipe")) {
            for (String id : inputs(ResourceFiles.parse(file))) {
                if (tagOf.containsKey(id)) {
                    failures.add(ResourceFiles.describe(file) + " -> asks for " + id + "; use #" + tagOf.get(id));
                }
            }
        }

        assertThat(failures).as("recipes naming one of our gears as an input").isEmpty();
    }

    /** Each of our items in a {@code c:gears/<material>} tag, mapped to that tag. */
    private static Map<String, String> gearTags() {
        Path root = ResourceFiles.dataRoot().resolve("c/tags/item/gears");
        Map<String, String> tagOf = new LinkedHashMap<>();
        for (Path file : ResourceFiles.jsonFilesUnder(root)) {
            String name = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            String tag = "c:gears/" + name.substring(0, name.length() - ".json".length());
            for (JsonElement entry : ResourceFiles.parse(file).getAsJsonArray("values")) {
                String value = entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString();
                if (value.startsWith(ResourceFiles.NAMESPACE + ":")) {
                    tagOf.put(value, tag);
                }
            }
        }
        return tagOf;
    }

    private static List<String> inputs(JsonElement element) {
        List<String> found = new ArrayList<>();
        collectInputs(element, found);
        return found;
    }

    private static void collectInputs(JsonElement element, List<String> found) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (!OUTPUT_KEYS.contains(entry.getKey())) {
                    collectInputs(entry.getValue(), found);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectInputs(child, found));
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            found.add(element.getAsString());
        }
    }
}
