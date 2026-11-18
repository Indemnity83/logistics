package com.logistics.resource.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Loot tables use the format of this branch's Minecraft version.
 *
 * <p>MC 26.3 writes a pool or entry condition as {@code "condition": {"type": ...}} and an item
 * function as {@code "modifier": {...}}. Earlier versions write {@code "conditions": [{"condition":
 * "..."}]} and {@code "functions": [{"function": ...}]}, and silently ignore a {@code "modifier"}: the
 * table still loads, but the function never runs. A loot table cherry-picked across that line
 * therefore keeps working in game while dropping, say, a machine's saved tier.
 */
@DisplayName("Loot table format contract")
class LootTableFormatContractTest {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)");

    @Test
    @DisplayName("every loot table uses this Minecraft version's format")
    void everyLootTableUsesThisVersionsFormat() {
        boolean singular = usesSingularFormat(minecraftVersion());
        List<String> failures = new ArrayList<>();

        for (Path file : lootTables()) {
            List<String> found = new ArrayList<>();
            collect(ResourceFiles.parse(file), singular, found);
            for (String problem : found) {
                failures.add(ResourceFiles.describe(file) + " -> " + problem);
            }
        }

        assertThat(failures)
                .as("loot tables in the wrong format for Minecraft %s", minecraftVersion())
                .isEmpty();
    }

    /** 26.3 and later use the singular {@code condition}/{@code modifier} format. */
    static boolean usesSingularFormat(String version) {
        Matcher matcher = VERSION.matcher(version);
        if (!matcher.find()) {
            throw new IllegalStateException("unrecognised minecraft_version '" + version + "'");
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        return major > 26 || (major == 26 && minor >= 3);
    }

    private static void collect(JsonElement element, boolean singular, List<String> found) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, singular, found));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (singular && (key.equals("conditions") || key.equals("functions"))) {
                found.add("'" + key + "' is the pre-26.3 form; use '"
                        + (key.equals("conditions") ? "condition" : "modifier") + "'");
            } else if (!singular && key.equals("modifier")) {
                found.add("'modifier' is ignored before 26.3; use 'functions': [{\"function\": ...}]");
            } else if (!singular && key.equals("condition") && value.isJsonObject()) {
                found.add("'condition': {...} is the 26.3 form; use 'conditions': [{\"condition\": ...}]");
            }
            collect(value, singular, found);
        }
    }

    /** Every loot table common and each loader ship. */
    private static List<Path> lootTables() {
        List<Path> roots = new ArrayList<>();
        roots.add(ResourceFiles.dataRoot());
        for (String module : ResourceFiles.LOADER_MODULES) {
            roots.add(repositoryRoot().resolve(module).resolve("src/main/resources/data"));
        }
        List<Path> tables = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(path -> path.toString().replace('\\', '/').contains("/loot_table/"))
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted()
                        .forEach(tables::add);
            } catch (IOException e) {
                throw new UncheckedIOException("could not walk " + root, e);
            }
        }
        assertThat(tables).as("loot tables found").isNotEmpty();
        return tables;
    }

    private static String minecraftVersion() {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(repositoryRoot().resolve("gradle.properties"))) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read gradle.properties", e);
        }
        String version = properties.getProperty("minecraft_version");
        if (version == null) {
            throw new IllegalStateException("gradle.properties has no minecraft_version");
        }
        return version.trim();
    }

    /** The directory holding {@code settings.gradle}, found by walking up from the shipped data. */
    private static Path repositoryRoot() {
        for (Path candidate = ResourceFiles.dataRoot(); candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("settings.gradle"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("could not find the repository root above " + ResourceFiles.dataRoot());
    }
}
