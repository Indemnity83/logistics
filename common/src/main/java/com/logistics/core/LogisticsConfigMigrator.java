package com.logistics.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigKey;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.crash.CrashReporting;
import com.logistics.core.lib.platform.PlatformService;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-time migrator from the pre-configory god-config ({@code config/logistics.json}) to the per-domain
 * configory files ({@code config/logistics/*.json}).
 *
 * <p>Domain-contributed: each domain registers its own legacy→key mappings via {@link #mapLegacy} /
 * {@link #mapLegacyPair} during {@code DomainBootstrap.registerConfig()}, so this class imports no domain
 * types — the mapping table is built from typed {@link ConfigKey}s the domains supply. {@link #migrateIfNeeded()}
 * (run once after config load) reads the legacy file, applies every mapping, saves all configs, and renames the
 * legacy file to {@code logistics.json.bak}. Best-effort: a failure is logged and never aborts startup (defaults
 * are already loaded).
 */
public final class LogisticsConfigMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/config");

    private record Scalar(String group, String field, ConfigKey<?> key) {}

    private static final List<Scalar> SCALARS = new ArrayList<>();
    private static final List<Consumer<JsonObject>> PAIRS = new ArrayList<>();

    private LogisticsConfigMigrator() {}

    /** Register a legacy {@code group.field} → {@code key} mapping (called by domains at config registration). */
    public static void mapLegacy(String group, String field, ConfigKey<?> key) {
        SCALARS.add(new Scalar(group, field, key));
    }

    /**
     * Register a legacy min/max pair mapping. The captured {@code T} lets the applier call the typed
     * {@link Config#repairMinMax} after raw-setting both members, so a widened value isn't rejected against the
     * still-default sibling and an inverted on-disk pair is healed rather than dropped.
     */
    public static <T extends Number & Comparable<T>> void mapLegacyPair(
            String group, String minField, String maxField, ConfigKey<T> minKey, ConfigKey<T> maxKey) {
        PAIRS.add(root -> applyPair(root, group, minField, maxField, minKey, maxKey));
    }

    /** Migrate {@code config/logistics.json} into the per-domain configory files, if it still exists. */
    public static void migrateIfNeeded() {
        Path legacy = PlatformService.INSTANCE.configDir().resolve("logistics.json");
        if (!Files.exists(legacy)) {
            return;
        }
        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(legacy)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            }
            apply(root);
            LogisticsConfigHost.domainConfigs().forEach(Config::save);
            // A migrated crash_reporting_enabled=true should take effect this session, not just next launch.
            CrashReporting.reconcile();
            Files.move(legacy, legacy.resolveSibling("logistics.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Migrated legacy logistics.json into per-domain config; renamed it to logistics.json.bak");
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy logistics.json; continuing with current config", e);
        }
    }

    /** Apply every registered legacy mapping to {@code root} (in memory). Package-visible for tests. */
    static void apply(JsonObject root) {
        for (Scalar scalar : SCALARS) {
            applyScalar(root, scalar);
        }
        for (Consumer<JsonObject> pair : PAIRS) {
            pair.accept(root);
        }
    }

    /** Clear the registered mappings — test isolation only. */
    static void clearMappingsForTest() {
        SCALARS.clear();
        PAIRS.clear();
    }

    private static void applyScalar(JsonObject root, Scalar scalar) {
        JsonElement value = valueAt(root, scalar.group(), scalar.field());
        if (value == null) {
            return;
        }
        Config config = ConfigRegistry.config(scalar.key().configId());
        if (!config.trySet(scalar.key().path().fullPath(), coerce(scalar.key(), value))) {
            LOGGER.warn("Skipped invalid legacy config value {}.{} during migration", scalar.group(), scalar.field());
        }
    }

    private static <T extends Number & Comparable<T>> void applyPair(
            JsonObject root, String group, String minField, String maxField, ConfigKey<T> minKey, ConfigKey<T> maxKey) {
        JsonElement minValue = valueAt(root, group, minField);
        JsonElement maxValue = valueAt(root, group, maxField);
        if (minValue == null && maxValue == null) {
            return;
        }
        Config config = ConfigRegistry.config(minKey.configId());
        if (minValue != null) {
            config.set(minKey.path().fullPath(), coerce(minKey, minValue));
        }
        if (maxValue != null) {
            config.set(maxKey.path().fullPath(), coerce(maxKey, maxValue));
        }
        config.repairMinMax(minKey, maxKey);
    }

    private static JsonElement valueAt(JsonObject root, String group, String field) {
        if (!root.has(group) || !root.get(group).isJsonObject()) {
            return null;
        }
        JsonObject groupObject = root.getAsJsonObject(group);
        if (!groupObject.has(field) || groupObject.get(field).isJsonNull()) {
            return null;
        }
        return groupObject.get(field);
    }

    private static Object coerce(ConfigKey<?> key, JsonElement value) {
        return switch (key.definition().type()) {
            case BOOLEAN -> value.getAsBoolean();
            case STRING, ENUM -> value.getAsString();
            case INT -> value.getAsInt();
            case LONG -> value.getAsLong();
            case FLOAT -> value.getAsFloat();
            case DOUBLE -> value.getAsDouble();
        };
    }
}
