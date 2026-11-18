package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every texture slot a model inherits resolves, vanilla parents included.
 *
 * <p>A vanilla parent can map a slot to a variable the child never defines, e.g.
 * {@code orientable_with_bottom}'s {@code "north": "#front"}. Minecraft then logs "Unresolved texture
 * references" on every resource load even though the child's own faces render fine. The other model
 * contracts stop at vanilla parents; this one reads them from the client jar on the test classpath.
 *
 * <p>Only rendered models count, as only those are resolved in game: the ones a blockstate or item
 * definition names. A template that leaves a slot for its children to fill is not an error.
 */
@DisplayName("Model texture slot contract")
class ModelTextureSlotContractTest {

    @Test
    @DisplayName("every texture slot in a model's parent chain resolves")
    void everyInheritedTextureSlotResolves() {
        List<String> failures = new ArrayList<>();

        for (Path file : renderedModels()) {
            Map<String, String> slots = new LinkedHashMap<>();
            List<JsonObject> chain = chainOf(ResourceFiles.parse(file), ResourceFiles.describe(file), failures);
            for (int i = chain.size() - 1; i >= 0; i--) {
                JsonElement textures = chain.get(i).get("textures");
                if (textures != null && textures.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : textures.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            slots.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }
            for (Map.Entry<String, String> slot : slots.entrySet()) {
                String unresolved = unresolvedVariable(slot.getValue(), slots);
                if (unresolved != null) {
                    failures.add(ResourceFiles.describe(file) + " -> #" + slot.getKey() + " -> #" + unresolved
                            + " is defined nowhere in the parent chain");
                }
            }
        }

        assertThat(failures).as("model texture slots that resolve to nothing").isEmpty();
    }

    /**
     * Models a blockstate or item definition names. Minecraft 1.21.1 has no item definitions and
     * renders every {@code models/item} model directly.
     */
    private static List<Path> renderedModels() {
        Set<String> references = new LinkedHashSet<>();
        List<Path> sources = new ArrayList<>(ResourceFiles.jsonFiles("blockstates"));
        boolean itemDefinitions = Files.isDirectory(ResourceFiles.assetRoot().resolve("items"));
        if (itemDefinitions) {
            sources.addAll(ResourceFiles.jsonFiles("items"));
        }
        for (Path file : sources) {
            references.addAll(ResourceFiles.collectStrings(ResourceFiles.parse(file), Set.of("model", "base")));
        }
        Set<Path> models = new LinkedHashSet<>();
        for (String reference : references) {
            try {
                Path target = ResourceFiles.resolve(reference, "models", ".json");
                if (target != null && Files.isRegularFile(target)) {
                    models.add(target);
                }
            } catch (ResourceFiles.BadReferenceException e) {
                // reported by the blockstate and item-definition contracts
            }
        }
        if (!itemDefinitions) {
            models.addAll(ResourceFiles.jsonFiles("models/item"));
        }
        assertThat(models).as("rendered models found").isNotEmpty();
        return new ArrayList<>(models);
    }

    /** The variable a {@code #...} chain ends on when nothing defines it; null when it resolves. */
    private static String unresolvedVariable(String value, Map<String, String> slots) {
        Set<String> seen = new HashSet<>();
        String current = value;
        while (current.startsWith("#")) {
            String name = current.substring(1);
            if (!seen.add(name)) {
                return name; // a loop resolves to nothing either
            }
            String next = slots.get(name);
            if (next == null) {
                return name;
            }
            current = next;
        }
        return null;
    }

    /** {@code model} followed by its parents, ours from the shipped assets and Minecraft's from its jar. */
    private static List<JsonObject> chainOf(JsonObject model, String described, List<String> failures) {
        List<JsonObject> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JsonObject current = model;
        while (current != null) {
            chain.add(current);
            if (!current.has("parent")) {
                break;
            }
            String parent = current.get("parent").getAsString();
            if (!seen.add(parent) || parent.startsWith("builtin/") || parent.startsWith("minecraft:builtin/")) {
                break;
            }
            current = load(parent, described, failures);
        }
        return chain;
    }

    private static JsonObject load(String reference, String described, List<String> failures) {
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);
        if (namespace.equals("minecraft")) {
            String resource = "assets/minecraft/models/" + path + ".json";
            try (InputStream in = ModelTextureSlotContractTest.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    failures.add(described + " -> vanilla parent '" + reference
                            + "' is not on the test classpath; this contract cannot check it");
                    return null;
                }
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            Path target = ResourceFiles.resolve(reference, "models", ".json");
            return target != null && Files.isRegularFile(target) ? ResourceFiles.parse(target) : null;
        } catch (ResourceFiles.BadReferenceException e) {
            return null; // ModelReferenceContractTest reports bad parents
        }
    }
}
