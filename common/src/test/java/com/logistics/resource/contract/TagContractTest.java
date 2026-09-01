package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Shipped tags reference things that exist and do not loop.
 *
 * <p>Tags are keyed {@code <namespace>:<registry>:<path>} from their file location, because our tag
 * files live under the {@code c} and {@code minecraft} namespaces rather than our own.
 */
@DisplayName("Tag contract")
class TagContractTest {

    /** Convention tags we reference but do not ship; a loader provides them. */
    private static final Set<String> EXTERNAL_TAGS = Set.of(
        "c:worldgen/biome:is_desert",
        "c:worldgen/biome:is_badlands",
        "c:worldgen/biome:is_savanna");

    @Test
    @DisplayName("every item a tag lists is shipped")
    void everyTaggedItemIsShipped() {
        Set<String> shipped = ResourceFiles.itemDefinitionIds();
        List<String> failures = new ArrayList<>();

        forEachTag((id, file, values) -> {
            // Only item tags hold item ids. Block, fluid, and worldgen tags name entries from their
            // own registry, which the shipped item definitions say nothing about.
            if (!"item".equals(registryOf(id))) {
                return;
            }
            for (String value : values) {
                if (value.startsWith("#") || !value.startsWith(ResourceFiles.NAMESPACE + ":")) {
                    continue;
                }
                if (!shipped.contains(value)) {
                    failures.add(ResourceFiles.describe(file) + " -> lists unknown item '" + value + "'");
                }
            }
        });

        assertThat(failures).as("tags listing items we do not ship").isEmpty();
    }

    @Test
    @DisplayName("every tag a tag references is shipped or explicitly external")
    void everyReferencedTagResolves() {
        Map<String, Path> tags = shippedTags();
        List<String> failures = new ArrayList<>();

        forEachTag((id, file, values) -> {
            for (String value : values) {
                if (!value.startsWith("#")) {
                    continue;
                }
                String referenced = tagId(namespaceOf(value.substring(1)), registryOf(id), pathOf(value.substring(1)));
                if (!tags.containsKey(referenced) && !EXTERNAL_TAGS.contains(referenced)) {
                    failures.add(ResourceFiles.describe(file) + " -> references unknown tag '" + value
                        + "'; if a loader provides it, add \"" + referenced + "\" to EXTERNAL_TAGS with a reason");
                }
            }
        });

        assertThat(failures).as("unresolved tag references").isEmpty();
    }

    @Test
    @DisplayName("no tag reference chain loops")
    void noTagReferenceChainLoops() {
        Map<String, Path> tags = shippedTags();
        List<String> failures = new ArrayList<>();

        for (String start : tags.keySet()) {
            Set<String> seen = new LinkedHashSet<>();
            if (loops(start, tags, seen, new HashSet<>())) {
                failures.add(start + " -> tag reference chain loops: " + seen);
            }
        }

        assertThat(failures).as("looping tag references").isEmpty();
    }

    /** Sanity guard, not a coverage claim: catches walking an empty or wrong directory. */
    @Test
    @DisplayName("the shipped tag set has not collapsed")
    void tagSetHasNotCollapsed() {
        assertThat(shippedTags())
            .as("shipped tags; lower this floor deliberately if tags were removed")
            .hasSizeGreaterThanOrEqualTo(37);
    }

    private boolean loops(String id, Map<String, Path> tags, Set<String> path, Set<String> done) {
        if (path.contains(id)) {
            return true;
        }
        if (!done.add(id) || !tags.containsKey(id)) {
            return false;
        }
        path.add(id);
        for (String value : valuesOf(tags.get(id))) {
            if (!value.startsWith("#")) {
                continue;
            }
            String ref = tagId(namespaceOf(value.substring(1)), registryOf(id), pathOf(value.substring(1)));
            if (loops(ref, tags, path, done)) {
                return true;
            }
        }
        path.remove(id);
        return false;
    }

    /** Every shipped tag, keyed {@code namespace:registry:path} from its file location. */
    private static Map<String, Path> shippedTags() {
        if (shippedTags == null) {
            shippedTags = scanTags();
        }
        return shippedTags;
    }

    // Scanning walks the whole shipped data tree, so hold the result across the tests in this class.
    private static Map<String, Path> shippedTags;

    private static Map<String, Path> scanTags() {
        Map<String, Path> tags = new LinkedHashMap<>();
        Path root = ResourceFiles.dataRoot();
        for (Path file : ResourceFiles.jsonFilesUnder(root)) {
            // <namespace>/tags/<registry>/<path>.json, where <path> may itself contain slashes
            String relative = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
            String[] parts = relative.split("/", 3);
            if (parts.length < 3 || !"tags".equals(parts[1])) {
                continue;
            }
            String rest = parts[2].substring(0, parts[2].length() - ".json".length());
            // Most registry directories are a single segment ("item", "block"); the worldgen ones
            // are nested ("worldgen/biome"). Everything after the registry is the tag's own path.
            int segments = rest.startsWith("worldgen/") ? 2 : 1;
            int split = -1;
            for (int i = 0; i < segments; i++) {
                split = rest.indexOf('/', split + 1);
                if (split < 0) {
                    break;
                }
            }
            if (split < 0) {
                continue;
            }
            tags.put(tagId(parts[0], rest.substring(0, split), rest.substring(split + 1)), file);
        }
        return tags;
    }

    private void forEachTag(TagConsumer consumer) {
        shippedTags().forEach((id, file) -> consumer.accept(id, file, valuesOf(file)));
    }

    private static List<String> valuesOf(Path file) {
        List<String> values = new ArrayList<>();
        JsonElement raw = ResourceFiles.parse(file).get("values");
        if (raw == null || !raw.isJsonArray()) {
            return values;
        }
        for (JsonElement entry : raw.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                values.add(entry.getAsString());
            } else if (entry.isJsonObject() && entry.getAsJsonObject().has("id")) {
                values.add(entry.getAsJsonObject().get("id").getAsString());
            }
        }
        return values;
    }

    private static String tagId(String namespace, String registry, String path) {
        return namespace + ":" + registry + ":" + path;
    }

    private static String namespaceOf(String reference) {
        int colon = reference.indexOf(':');
        return colon < 0 ? "minecraft" : reference.substring(0, colon);
    }

    private static String pathOf(String reference) {
        int colon = reference.indexOf(':');
        return colon < 0 ? reference : reference.substring(colon + 1);
    }

    /** A tag reference stays within the registry of the tag that names it. */
    private static String registryOf(String id) {
        String[] parts = id.split(":", 3);
        return parts[1];
    }

    @FunctionalInterface
    private interface TagConsumer {
        void accept(String id, Path file, List<String> values);
    }
}
