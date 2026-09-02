package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /**
     * Every texture variable a rendered model's geometry uses is defined somewhere in its chain.
     *
     * <p>The checks above follow references a model <em>declares</em>. This one covers what a model
     * <em>needs</em>: a face pointing at {@code #side} when nothing defines {@code side} renders as
     * the missing texture, and no amount of validating declared references reveals it.
     */
    @Test
    @DisplayName("every rendered model defines the texture variables its faces use")
    void everyRenderedModelDefinesItsTextureVariables() {
        List<String> failures = new ArrayList<>();

        for (Path file : renderedModels()) {
            List<JsonObject> chain = chainOf(file);
            Map<String, String> textures = resolvedTextures(chain);
            for (String variable : variablesUsedBy(chain)) {
                if (!textures.containsKey(variable)) {
                    failures.add(ResourceFiles.describe(file) + " -> uses '#" + variable
                        + "' but nothing in its parent chain defines it");
                }
            }
        }

        assertThat(failures).as("model texture variables with no definition").isEmpty();
    }

    /**
     * Every rendered model with geometry supplies a {@code particle} texture.
     *
     * <p>{@code particle} is the one texture a model needs without ever naming it — it is not reached
     * through a {@code #variable}, so nothing else here can see it missing. Without it, break and
     * landing particles fall back to the missing texture.
     *
     * <p>Validated against the real failure: run over the tree before the fix that added these, this
     * flags exactly the 17 models Minecraft warned about, and no others.
     */
    @Test
    @DisplayName("every rendered model with geometry supplies a particle texture")
    void everyRenderedModelWithGeometrySuppliesParticle() {
        List<String> failures = new ArrayList<>();

        for (Path file : renderedModels()) {
            List<JsonObject> chain = chainOf(file);
            if (!hasGeometry(chain) || resolvedTextures(chain).containsKey("particle")) {
                continue;
            }
            failures.add(ResourceFiles.describe(file) + " -> has geometry but no 'particle' texture");
        }

        assertThat(failures).as("models whose break particles fall back to the missing texture").isEmpty();
    }

    /**
     * Models actually used for rendering: those a blockstate or item definition names.
     *
     * <p>Templates are deliberately excluded. A model that only exists to be inherited from — our
     * tank and marker-torch bases — legitimately leaves variables and {@code particle} to whichever
     * child supplies them, so requiring them of every file on disk would report false failures and
     * invite weakening these checks.
     */
    private static Set<Path> renderedModels() {
        Set<String> references = new LinkedHashSet<>();
        for (String directory : List.of("blockstates", "items")) {
            for (Path file : ResourceFiles.jsonFiles(directory)) {
                collectModelReferences(ResourceFiles.parse(file), references);
            }
        }

        Set<Path> models = new LinkedHashSet<>();
        for (String reference : references) {
            try {
                Path target = ResourceFiles.resolve(reference, "models", ".json");
                if (target != null && Files.isRegularFile(target)) {
                    models.add(target);
                }
            } catch (ResourceFiles.BadReferenceException e) {
                // Reported by the blockstate and item-definition contracts; not this test's job.
            }
        }
        return models;
    }

    /** Any string-valued "model" or "base", at any depth — covers every definition shape. */
    private static void collectModelReferences(JsonElement node, Set<String> into) {
        if (node.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : node.getAsJsonObject().entrySet()) {
                boolean isModel = ("model".equals(entry.getKey()) || "base".equals(entry.getKey()))
                    && entry.getValue().isJsonPrimitive();
                if (isModel) {
                    into.add(entry.getValue().getAsString());
                } else {
                    collectModelReferences(entry.getValue(), into);
                }
            }
        } else if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(child -> collectModelReferences(child, into));
        }
    }

    /**
     * The model and its parents, nearest first, stopping at the first parent we do not ship.
     *
     * <p>Vanilla parents end the walk: resolving what {@code minecraft:block/cube} defines would need
     * a version-matched index of Minecraft's own models, the same limit the namespace policy states.
     * A variable that only a vanilla ancestor supplies is therefore out of reach here.
     */
    private static List<JsonObject> chainOf(Path file) {
        List<JsonObject> chain = new ArrayList<>();
        Set<Path> seen = new LinkedHashSet<>();
        Path current = file;
        while (current != null && Files.isRegularFile(current) && seen.add(current)) {
            JsonObject model = ResourceFiles.parse(current);
            chain.add(model);
            if (!model.has("parent")) {
                break;
            }
            try {
                current = ResourceFiles.resolve(model.get("parent").getAsString(), "models", ".json");
            } catch (ResourceFiles.BadReferenceException e) {
                break;
            }
        }
        return chain;
    }

    /** Textures visible to the leaf model: parents first, so a child's entry wins. */
    private static Map<String, String> resolvedTextures(List<JsonObject> chain) {
        Map<String, String> textures = new LinkedHashMap<>();
        for (int i = chain.size() - 1; i >= 0; i--) {
            JsonElement declared = chain.get(i).get("textures");
            if (declared == null || !declared.isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : declared.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    textures.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        return textures;
    }

    /**
     * Variable names the chain references, without the leading '#'.
     *
     * <p>Faces are the obvious source, but a {@code textures} entry may itself be a variable — our
     * tank base sets {@code "particle": "#side"} and lets each child supply {@code side}. Those count
     * too: a {@code particle} aliased to a variable nothing defines is as broken as a missing one, and
     * checking only that the {@code particle} key exists would wave it through.
     */
    private static Set<String> variablesUsedBy(List<JsonObject> chain) {
        Set<String> variables = new LinkedHashSet<>();
        forEachFace(chain, face -> {
            JsonElement texture = face.get("texture");
            if (texture != null && texture.isJsonPrimitive() && texture.getAsString().startsWith("#")) {
                variables.add(texture.getAsString().substring(1));
            }
        });
        for (String value : resolvedTextures(chain).values()) {
            if (value.startsWith("#")) {
                variables.add(value.substring(1));
            }
        }
        return variables;
    }

    private static boolean hasGeometry(List<JsonObject> chain) {
        for (JsonObject model : chain) {
            JsonElement elements = model.get("elements");
            if (elements != null && elements.isJsonArray() && !elements.getAsJsonArray().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void forEachFace(List<JsonObject> chain, java.util.function.Consumer<JsonObject> action) {
        for (JsonObject model : chain) {
            JsonElement elements = model.get("elements");
            if (elements == null || !elements.isJsonArray()) {
                continue;
            }
            for (JsonElement element : elements.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonElement faces = element.getAsJsonObject().get("faces");
                if (faces == null || !faces.isJsonObject()) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> face : faces.getAsJsonObject().entrySet()) {
                    if (face.getValue().isJsonObject()) {
                        action.accept(face.getValue().getAsJsonObject());
                    }
                }
            }
        }
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
