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
 * Metals other mods also make are interchangeable with ours: a recipe asks for the conventional
 * tag, never our item, so another mod's tin or bronze works anywhere ours does.
 */
@DisplayName("Shared metal tag contract")
class SharedMetalTagContractTest {

    /** Metals that other mods also add, by the material name their {@code c:} tags end in. */
    private static final Set<String> SHARED_METALS = Set.of("tin", "raw_tin", "bronze");

    /** Tag forms covered. Gears are excluded; gear recipes still name our gears directly. */
    private static final Set<String> FORMS = Set.of("ingots", "nuggets", "ores", "raw_materials", "storage_blocks");

    /** Recipe keys holding what a recipe produces; everything else a recipe names is an input. */
    private static final Set<String> OUTPUT_KEYS = Set.of("result", "byproduct");

    @Test
    @DisplayName("no recipe asks for our tin or bronze by exact id")
    void recipesAskForSharedMetalTags() {
        Map<String, String> tagOf = sharedMetalTags();
        assertThat(tagOf).as("our items in shared metal tags; the tag scan found nothing").isNotEmpty();

        List<String> failures = new ArrayList<>();
        for (Path file : ResourceFiles.dataJsonFiles("logistics/recipe")) {
            for (String id : inputs(ResourceFiles.parse(file))) {
                if (tagOf.containsKey(id)) {
                    failures.add(ResourceFiles.describe(file) + " -> asks for " + id + "; use #" + tagOf.get(id));
                }
            }
        }

        assertThat(failures).as("recipes naming our tin or bronze as an input").isEmpty();
    }

    /** Each of our items in a {@code c:<form>/<shared metal>} tag, mapped to that tag. */
    private static Map<String, String> sharedMetalTags() {
        Path root = ResourceFiles.dataRoot().resolve("c/tags/item");
        Map<String, String> tagOf = new LinkedHashMap<>();
        for (Path file : ResourceFiles.jsonFilesUnder(root)) {
            String name = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            name = name.substring(0, name.length() - ".json".length());
            String material = name.substring(name.lastIndexOf('/') + 1);
            if (!name.contains("/")
                    || !FORMS.contains(name.substring(0, name.indexOf('/')))
                    || !SHARED_METALS.contains(material)) {
                continue;
            }
            for (JsonElement entry : ResourceFiles.parse(file).getAsJsonArray("values")) {
                String value = entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString();
                if (value.startsWith(ResourceFiles.NAMESPACE + ":")) {
                    tagOf.put(value, "c:" + name);
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
