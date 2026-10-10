package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Our dusts are interchangeable with other mods' dusts of the same material: each sits in its
 * conventional {@code c:dusts/<material>} tag, and recipes ask for the tag rather than our item.
 */
@DisplayName("Dust tag contract")
class DustTagContractTest {

    private static final Pattern DUST = Pattern.compile("logistics:core/[a-z_]+_dust");

    /** Recipe keys holding what a recipe produces; everything else a recipe names is an input. */
    private static final Set<String> OUTPUT_KEYS = Set.of("result", "byproduct");

    @Test
    @DisplayName("every dust we ship is in a c:dusts tag that c:dusts includes")
    void everyDustIsConventionallyTagged() {
        Path dustTags = ResourceFiles.dataRoot().resolve("c/tags/item/dusts");
        List<String> parent = values(ResourceFiles.dataRoot().resolve("c/tags/item/dusts.json"));
        Set<String> tagged = new TreeSet<>();
        List<String> failures = new ArrayList<>();

        for (Path file : ResourceFiles.jsonFilesUnder(dustTags)) {
            String name = dustTags.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            String tag = "#c:dusts/" + name.substring(0, name.length() - ".json".length());
            if (!parent.contains(tag)) {
                failures.add("c:dusts does not include " + tag);
            }
            tagged.addAll(values(file));
        }
        for (String item : ResourceFiles.itemDefinitionIds()) {
            if (DUST.matcher(item).matches() && !tagged.contains(item)) {
                failures.add(item + " is in no c:dusts/* tag");
            }
        }

        assertThat(failures).as("dusts missing from their conventional tags").isEmpty();
    }

    @Test
    @DisplayName("no recipe asks for one of our dusts by exact id")
    void recipesAskForDustTags() {
        List<String> failures = new ArrayList<>();
        for (Path file : ResourceFiles.dataJsonFiles("logistics/recipe")) {
            for (String id : inputs(ResourceFiles.parse(file))) {
                if (DUST.matcher(id).matches()) {
                    failures.add(ResourceFiles.describe(file) + " -> asks for " + id + "; use its c:dusts tag");
                }
            }
        }

        assertThat(failures).as("recipes naming one of our dusts as an input").isEmpty();
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

    private static List<String> values(Path tagFile) {
        List<String> values = new ArrayList<>();
        if (!Files.isRegularFile(tagFile)) {
            return values;
        }
        for (JsonElement entry : ResourceFiles.parse(tagFile).getAsJsonArray("values")) {
            values.add(entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString());
        }
        return values;
    }
}
