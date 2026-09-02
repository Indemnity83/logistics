package com.logistics.resource.contract;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Locates shipped resources on the test classpath, parses them, and turns a reference string
 * into the file it must resolve to. Reads JSON only; never boots a server.
 */
final class ResourceFiles {

    static final String NAMESPACE = "logistics";

    /**
     * Namespaces accepted without resolving the reference. Proving a vanilla asset exists would
     * need a version-matched index of Minecraft's own resources, which is not available here.
     */
    private static final Set<String> TRUSTED_NAMESPACES = Set.of("minecraft");

    /** References that intentionally resolve to no shipped file, each keyed to its reason. */
    private static final Map<String, String> ALLOWED_UNRESOLVED = Map.of();

    /** Resolved once: these are called per file and per reference. */
    private static final Path ASSET_ROOT = locate("assets");

    // The namespace directory's parent, so tags under `c` and `minecraft` are reachable too.
    private static final Path DATA_ROOT = locate("data").getParent();

    private ResourceFiles() {}

    /** Root of the shipped assets tree on the test classpath ({@code .../assets/logistics}). */
    static Path assetRoot() {
        return ASSET_ROOT;
    }

    private static Path locate(String tree) {
        URL url = ResourceFiles.class.getClassLoader().getResource(tree + "/" + NAMESPACE);
        if (url == null) {
            throw new IllegalStateException(
                tree + "/" + NAMESPACE + " is not on the test classpath; "
                    + "resource contract tests cannot run without the shipped resources");
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("could not resolve the " + tree + " root", e);
        }
    }

    /** Root of the shipped data tree on the test classpath ({@code .../data}). */
    static Path dataRoot() {
        return DATA_ROOT;
    }

    /** Loader modules that may ship a resource common deliberately leaves out. */
    static final List<String> LOADER_MODULES = List.of("fabric", "neoforge");

    /**
     * {@code <module>/src/main/resources/assets/logistics}. Loader resources are not on the common
     * test classpath, so this walks up to the repository root and back down.
     */
    static Path loaderAssetRoot(String module) {
        Path root = repositoryRoot().resolve(module).resolve("src/main/resources/assets").resolve(NAMESPACE);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException(
                "expected loader assets at " + root + "; the module layout changed and this contract "
                    + "would silently stop checking loader-supplied resources");
        }
        return root;
    }

    /** The directory holding {@code settings.gradle}, found by walking up from the shipped assets. */
    private static Path repositoryRoot() {
        for (Path candidate = ASSET_ROOT; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))) {
                return candidate;
            }
        }
        throw new IllegalStateException(
            "could not find the repository root above " + ASSET_ROOT + "; loader resources are unreachable");
    }

    /** Every {@code .json} file under {@code assets/logistics/<dir>}, in stable order. */
    static List<Path> jsonFiles(String dir) {
        return jsonFilesUnder(assetRoot().resolve(dir));
    }

    /** Every {@code .json} file under {@code data/<dir>}, in stable order. */
    static List<Path> dataJsonFiles(String dir) {
        return jsonFilesUnder(dataRoot().resolve(dir));
    }

    static List<Path> jsonFilesUnder(Path root) {
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("expected a directory of shipped resources: " + root);
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("could not walk " + root, e);
        }
    }

    /**
     * Ids of every item we ship a definition for, as {@code logistics:path}. A proxy for "items
     * this mod has", not proof of registration: a definition can exist for an unregistered item.
     */
    static Set<String> itemDefinitionIds() {
        Path root = assetRoot().resolve("items");
        return jsonFilesUnder(root).stream()
            .map(file -> {
                String relative = root.relativize(file).toString().replace(java.io.File.separatorChar, '/');
                return NAMESPACE + ":" + relative.substring(0, relative.length() - ".json".length());
            })
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    static JsonObject parse(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + file, e);
        }
    }

    /** Path relative to the assets or data root, for readable failure messages. */
    static String describe(Path file) {
        Path base = assetRoot().getParent();
        return file.startsWith(base) ? base.relativize(file).toString() : dataRoot().relativize(file).toString();
    }

    /**
     * Turns a reference such as {@code logistics:block/kiln} into the file it must resolve to,
     * or returns {@code null} when the reference is trusted and deliberately unresolved.
     *
     * @param kind      resource directory, e.g. {@code models} or {@code textures}
     * @param extension file extension including the dot, e.g. {@code .json}
     * @throws BadReferenceException when the namespace is untrusted or the path escapes {@code kind}
     */
    static Path resolve(String reference, String kind, String extension) {
        // An unqualified reference means vanilla, matching Minecraft's own default.
        int colon = reference.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : reference.substring(0, colon);
        String path = colon < 0 ? reference : reference.substring(colon + 1);

        if (ALLOWED_UNRESOLVED.containsKey(reference)) {
            return null;
        }
        if (TRUSTED_NAMESPACES.contains(namespace)) {
            return null;
        }
        if (!NAMESPACE.equals(namespace)) {
            throw new UnexpectedNamespaceException(reference, namespace);
        }

        // Minecraft allows '.' and '/' in a resource path and only rejects ".." in the namespace,
        // so a reference can climb out of the resource directory. Left unchecked, such a reference
        // could land on an unrelated real file and be reported as resolved.
        Path directory = assetRoot().resolve(kind).normalize();
        Path target = directory.resolve(path + extension).normalize();
        if (!target.startsWith(directory)) {
            throw new EscapingReferenceException(reference, kind);
        }
        return target;
    }

    /** Collects every string value stored under any of {@code keys}, at any depth. */
    static List<String> collectStrings(JsonElement element, Set<String> keys) {
        List<String> found = new ArrayList<>();
        collectStrings(element, keys, found);
        return found;
    }

    private static void collectStrings(JsonElement element, Set<String> keys, List<String> found) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (keys.contains(entry.getKey()) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    found.add(value.getAsString());
                } else {
                    collectStrings(value, keys, found);
                }
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectStrings(child, keys, found));
        }
    }

    /** A reference that cannot be checked as written. */
    static class BadReferenceException extends RuntimeException {
        BadReferenceException(String message) {
            super(message);
        }
    }

    /** Raised when a reference names a namespace that is neither ours nor explicitly trusted. */
    static final class UnexpectedNamespaceException extends BadReferenceException {
        UnexpectedNamespaceException(String reference, String namespace) {
            super("unexpected namespace '" + namespace + "' in reference '" + reference
                + "'; add it to TRUSTED_NAMESPACES with a reason if this is intentional");
        }
    }

    /** Raised when a reference resolves outside the resource directory it names. */
    static final class EscapingReferenceException extends BadReferenceException {
        EscapingReferenceException(String reference, String kind) {
            super("reference '" + reference + "' resolves outside assets/" + NAMESPACE + "/" + kind);
        }
    }
}
